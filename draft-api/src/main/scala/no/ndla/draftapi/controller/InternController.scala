/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.implicits.toTry
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.database.DBUtility
import no.ndla.draftapi.DraftApiProperties
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{ArticleDomainDumpDTO, ArticleDumpDTO, ContentIdDTO, NotFoundException}
import no.ndla.draftapi.model.domain.{ArticleIds, ImportId}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.language.Language
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import no.ndla.search.model.domain.ReindexResult
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

import java.util.concurrent.{Executors, TimeUnit}
import scala.annotation.{tailrec, unused}
import scala.concurrent.*
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class InternController(using
    readService: => ReadService,
    writeService: WriteService,
    draftRepository: DraftRepository,
    articleIndexService: ArticleIndexService,
    tagIndexService: TagIndexService,
    articleApiClient: ArticleApiClient,
    props: DraftApiProperties,
    errorHandling: ErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: MyNDLAApiClient,
    dbUtility: DBUtility,
) extends TapirController
    with StrictLogging {
  override val prefix: EndpointInput[Unit] = "intern"
  override val enableSwagger               = false
  private val stringInternalServerError    = statusCode(StatusCode.InternalServerError).and(stringBody)

  def createIndexFuture(indexService: IndexService[?, ?], numShards: Option[Int])(implicit
      ec: ExecutionContext
  ): Future[Try[ReindexResult]] = {

    val fut = Future {
      indexService.indexDocuments(numShards)
    }

    val logEx = (ex: Throwable) => logger.error(s"Something went wrong when indexing ${indexService.documentType}:", ex)

    fut.onComplete {
      case Success(Success(result)) => logger.info(
          s"Successfully indexed ${result.totalIndexed} ${indexService.documentType}'s in ${result.millisUsed}ms"
        )
      case Failure(ex)          => logEx(ex)
      case Success(Failure(ex)) => logEx(ex)
    }

    fut
  }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    postIndex,
    deleteIndex,
    getIds,
    importExternalId,
    getByExternalId,
    getArticles,
    deleteArticle,
    dumpArticles,
    dumpSingleArticle,
    postDump,
  )

  def postIndex: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("index")
    .in(query[Option[Int]]("numShards"))
    .out(stringBody)
    .errorOut(stringInternalServerError)
    .serverLogicPure { numShards =>
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
      val articleIndex = createIndexFuture(articleIndexService, numShards)
      val tagIndex     = createIndexFuture(tagIndexService, numShards)
      val indexResults = Future.sequence(List(articleIndex, tagIndex))

      Await.result(indexResults, Duration.Inf).sequence match {
        case Failure(ex) =>
          logger.warn(ex.getMessage, ex)
          ex.getMessage.asLeft
        case Success(results) =>
          val result = results
            .map(rr => s"Completed indexing of ${rr.totalIndexed} ${rr.name}s in ${rr.millisUsed} ms.")
            .mkString("\n")
          logger.info(result)
          result.asRight
      }
    }

  def deleteIndexLogic(
      @unused
      x: Unit
  ): Either[String, String] = {
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    def pluralIndex(n: Int) =
      if (n == 1) "1 index"
      else s"$n indexes"

    val indexes = for {
      articleIndex <- Future {
        articleIndexService.findAllIndexes(props.DraftSearchIndex)
      }
      tagIndex <- Future {
        tagIndexService.findAllIndexes(props.DraftTagSearchIndex)
      }
    } yield (articleIndex, tagIndex)

    val deleteResults: Seq[Try[?]] = Await.result(indexes, Duration(10, TimeUnit.MINUTES)) match {
      case (Failure(articleFail), _)                      => return articleFail.getMessage.asLeft
      case (_, Failure(tagFail))                          => return tagFail.getMessage.asLeft
      case (Success(articleIndexes), Success(tagIndexes)) =>
        val articleDeleteResults = articleIndexes.map(index => {
          logger.info(s"Deleting article index $index")
          articleIndexService.deleteIndexWithName(Option(index))
        })
        val tagDeleteResults = tagIndexes.map(index => {
          logger.info(s"Deleting tag index $index")
          tagIndexService.deleteIndexWithName(Option(index))
        })
        articleDeleteResults ++ tagDeleteResults
    }

    val (errors, successes) = deleteResults.partition(_.isFailure)
    if (errors.nonEmpty) {
      val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
        s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
        s"${pluralIndex(successes.length)} were deleted successfully."
      message.asLeft
    } else {
      s"Deleted ${pluralIndex(successes.length)}".asRight
    }

  }

  def deleteIndex: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .in("index")
    .out(stringBody)
    .errorOut(stringInternalServerError)
    .serverLogicPure(deleteIndexLogic)

  def getIds: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("ids")
    .in(query[Option[String]]("status"))
    .errorOut(errorOutputsFor(400))
    .out(jsonBody[Seq[ArticleIds]])
    .serverLogicPure { status =>
      status.map(DraftStatus.valueOfOrError) match {
        case Some(Success(status)) => dbUtility.readOnly { implicit session =>
            draftRepository.idsWithStatus(status)
          }
        case Some(Failure(ex)) => Failure(ex)
        case None              => dbUtility.readOnly { implicit session =>
            draftRepository.getAllIds
          }
      }
    }

  def importExternalId: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("import-id" / path[String]("external_id"))
    .errorOut(errorOutputsFor(400, 404))
    .out(jsonBody[ImportId])
    .serverLogicPure(readService.importIdOfArticle)

  def getByExternalId: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("id" / path[String]("external_id"))
    .out(jsonBody[Long])
    .errorOut(errorOutputsFor(404))
    .serverLogicPure { externalId =>
      dbUtility.readOnly { implicit session =>
        draftRepository
          .getIdFromExternalId(externalId)
          .flatMap(_.toTry(NotFoundException(s"Could not find draft with external id: '$externalId'")))
      }
    }

  def getArticles: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("articles")
    .in(query[Int]("page").default(1))
    .in(query[Int]("page-size").default(250))
    .in(query[String]("language").default(Language.AllLanguages))
    .in(query[Boolean]("fallback").default(false))
    .out(jsonBody[ArticleDumpDTO])
    .errorOut(errorOutputsFor())
    .serverLogicPure(readService.getArticlesByPage)

  @tailrec
  private def deleteArticleWithRetries(
      id: Long,
      user: TokenUser,
      maxRetries: Int = 10,
      retries: Int = 0,
  ): Try[ContentIdDTO] = {
    articleApiClient.deleteArticle(id, user) match {
      case Failure(_) if retries <= maxRetries => deleteArticleWithRetries(id, user, maxRetries, retries + 1)
      case Failure(ex)                         => Failure(ex)
      case Success(x)                          => Success(x)
    }
  }

  def deleteArticle: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .in("article" / path[Long]("id"))
    .out(jsonBody[ContentIdDTO])
    .errorOut(errorOutputsFor(404))
    .requirePermission(DRAFT_API_WRITE)
    .serverLogicPure { user => id =>
      deleteArticleWithRetries(id, user).flatMap(id => writeService.deleteArticle(id.id))

    }

  def dumpArticles: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("dump" / "article")
    .in(query[Int]("page").default(1))
    .in(query[Int]("page-size").default(250))
    .out(jsonBody[ArticleDomainDumpDTO])
    .errorOut(errorOutputsFor())
    .serverLogicPure(readService.getArticleDomainDump)

  def dumpSingleArticle: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("dump" / "article" / path[Long]("id"))
    .errorOut(errorOutputsFor(404))
    .out(jsonBody[Draft])
    .serverLogicPure { id =>
      dbUtility.readOnly { implicit session =>
        draftRepository.withId(id).flatMap(_.toTry(NotFoundException(s"Could not find draft with id: '$id")))
      }
    }

  def postDump: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("dump" / "article")
    .in(jsonBody[Draft])
    .errorOut(errorOutputsFor(400, 500))
    .out(jsonBody[Draft])
    .serverLogicPure { article =>
      writeService.insertDump(article)
    }
}
