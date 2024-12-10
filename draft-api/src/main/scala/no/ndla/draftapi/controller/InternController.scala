/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.Props
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.{ArticleDomainDumpDTO, ArticleDumpDTO, ContentIdDTO, NotFoundException}
import no.ndla.draftapi.model.domain.{ArticleIds, ImportId}
import no.ndla.draftapi.repository.DraftRepository
import no.ndla.draftapi.service.*
import no.ndla.draftapi.service.search.*
import no.ndla.language.Language
import sttp.tapir.*
import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.tapir.NoNullJsonPrinter.*
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.TapirController
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.ReadOnlyAutoSession
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import io.circe.generic.auto.*
import no.ndla.search.model.domain.ReindexResult
import sttp.tapir.generic.auto.*

import java.util.concurrent.{Executors, TimeUnit}
import scala.annotation.{tailrec, unused}
import scala.concurrent.*
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: ReadService
    with WriteService
    with ConverterService
    with DraftRepository
    with IndexService
    with ArticleIndexService
    with TagIndexService
    with GrepCodesIndexService
    with ArticleApiClient
    with TapirController
    with Props =>
  val internController: InternController

  class InternController extends TapirController with StrictLogging {
    import props.{DraftSearchIndex, DraftTagSearchIndex, DraftGrepCodesSearchIndex}

    override val prefix: EndpointInput[Unit] = "intern"
    override val enableSwagger               = false
    private val stringInternalServerError    = statusCode(StatusCode.InternalServerError).and(stringBody)

    def createIndexFuture(
        indexService: IndexService[_, _],
        numShards: Option[Int]
    )(implicit ec: ExecutionContext): Future[Try[ReindexResult]] = {

      val fut = Future { indexService.indexDocuments(numShards) }

      val logEx = (ex: Throwable) =>
        logger.error(s"Something went wrong when indexing ${indexService.documentType}:", ex)

      fut.onComplete {
        case Success(Success(result)) =>
          logger.info(
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
      postDump
    )

    def postIndex: ServerEndpoint[Any, Eff] = endpoint.post
      .in("index")
      .in(query[Option[Int]]("numShards"))
      .out(stringBody)
      .errorOut(stringInternalServerError)
      .serverLogicPure { numShards =>
        implicit val ec: ExecutionContextExecutorService =
          ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
        val articleIndex = createIndexFuture(articleIndexService, numShards)
        val tagIndex     = createIndexFuture(tagIndexService, numShards)
        val grepIndex    = createIndexFuture(grepCodesIndexService, numShards)
        val indexResults = Future.sequence(List(articleIndex, tagIndex, grepIndex))

        Await.result(indexResults, Duration.Inf).sequence match {
          case Failure(ex) =>
            logger.warn(ex.getMessage, ex)
            ex.getMessage.asLeft
          case Success(results) =>
            val maxTime = results.map(rr => rr.millisUsed).max
            val result =
              s"Completed all indexes in ${maxTime} ms."
            logger.info(result)
            result.asRight
        }
      }

    def deleteIndexLogic(@unused x: Unit): Either[String, String] = {
      implicit val ec         = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"

      val indexes = for {
        articleIndex <- Future { articleIndexService.findAllIndexes(DraftSearchIndex) }
        tagIndex     <- Future { tagIndexService.findAllIndexes(DraftTagSearchIndex) }
        grepIndex    <- Future { grepCodesIndexService.findAllIndexes(DraftGrepCodesSearchIndex) }
      } yield (articleIndex, tagIndex, grepIndex)

      val deleteResults: Seq[Try[_]] = Await.result(indexes, Duration(10, TimeUnit.MINUTES)) match {
        case (Failure(articleFail), _, _) => return articleFail.getMessage.asLeft
        case (_, Failure(tagFail), _)     => return tagFail.getMessage.asLeft
        case (_, _, Failure(grepFail))    => return grepFail.getMessage.asLeft
        case (Success(articleIndexes), Success(tagIndexes), Success(grepIndexes)) =>
          val articleDeleteResults = articleIndexes.map(index => {
            logger.info(s"Deleting article index $index")
            articleIndexService.deleteIndexWithName(Option(index))
          })
          val tagDeleteResults = tagIndexes.map(index => {
            logger.info(s"Deleting tag index $index")
            tagIndexService.deleteIndexWithName(Option(index))
          })
          val grepDeleteResults = grepIndexes.map(index => {
            logger.info(s"Deleting grep index $index")
            grepCodesIndexService.deleteIndexWithName(Option(index))
          })
          articleDeleteResults ++ tagDeleteResults ++ grepDeleteResults
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

    def deleteIndex: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("index")
      .out(stringBody)
      .errorOut(stringInternalServerError)
      .serverLogicPure(deleteIndexLogic)

    def getIds: ServerEndpoint[Any, Eff] = endpoint.get
      .in("ids")
      .in(query[Option[String]]("status"))
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[Seq[ArticleIds]])
      .serverLogicPure { status =>
        status.map(DraftStatus.valueOfOrError) match {
          case Some(Success(status)) =>
            draftRepository.idsWithStatus(status)(ReadOnlyAutoSession).getOrElse(List.empty).asRight
          case Some(Failure(ex)) => returnLeftError(ex)
          case None              => draftRepository.getAllIds(ReadOnlyAutoSession).asRight
        }
      }

    def importExternalId: ServerEndpoint[Any, Eff] = endpoint.get
      .in("import-id" / path[String]("external_id"))
      .errorOut(errorOutputsFor(400, 404))
      .out(jsonBody[ImportId])
      .serverLogicPure { externalId =>
        readService.importIdOfArticle(externalId) match {
          case Some(ids) => ids.asRight
          case _         => ErrorHelpers.notFound.asLeft
        }
      }

    def getByExternalId: ServerEndpoint[Any, Eff] = endpoint.get
      .in("id" / path[String]("external_id"))
      .out(jsonBody[Long])
      .errorOut(errorOutputsFor(404))
      .serverLogicPure { externalId =>
        draftRepository.getIdFromExternalId(externalId)(ReadOnlyAutoSession) match {
          case Some(id) => id.asRight
          case None     => ErrorHelpers.notFound.asLeft
        }
      }

    def getArticles: ServerEndpoint[Any, Eff] = endpoint.get
      .in("articles")
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(250))
      .in(query[String]("language").default(Language.AllLanguages))
      .in(query[Boolean]("fallback").default(false))
      .out(jsonBody[ArticleDumpDTO])
      .serverLogicPure { case (pageNo, pageSize, lang, fallback) =>
        readService.getArticlesByPage(pageNo, pageSize, lang, fallback).asRight
      }

    @tailrec
    private def deleteArticleWithRetries(
        id: Long,
        user: TokenUser,
        maxRetries: Int = 10,
        retries: Int = 0
    ): Try[ContentIdDTO] = {
      articleApiClient.deleteArticle(id, user) match {
        case Failure(_) if retries <= maxRetries => deleteArticleWithRetries(id, user, maxRetries, retries + 1)
        case Failure(ex)                         => Failure(ex)
        case Success(x)                          => Success(x)
      }
    }

    def deleteArticle: ServerEndpoint[Any, Eff] = endpoint.delete
      .in("article" / path[Long]("id"))
      .out(jsonBody[ContentIdDTO])
      .errorOut(errorOutputsFor(404))
      .requirePermission(DRAFT_API_WRITE)
      .serverLogicPure { user => id =>
        deleteArticleWithRetries(id, user)
          .flatMap(id => writeService.deleteArticle(id.id))

      }

    def dumpArticles: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "article")
      .in(query[Int]("page").default(1))
      .in(query[Int]("page-size").default(250))
      .out(jsonBody[ArticleDomainDumpDTO])
      .serverLogicPure { case (pageNo, pageSize) =>
        readService.getArticleDomainDump(pageNo, pageSize).asRight
      }

    def dumpSingleArticle: ServerEndpoint[Any, Eff] = endpoint.get
      .in("dump" / "article" / path[Long]("id"))
      .errorOut(errorOutputsFor(404))
      .out(jsonBody[Draft])
      .serverLogicPure { id =>
        draftRepository.withId(id)(ReadOnlyAutoSession) match {
          case Some(article) => article.asRight
          case None          => returnLeftError(NotFoundException(s"Could not find draft with id: '$id"))
        }
      }

    def postDump: ServerEndpoint[Any, Eff] = endpoint.post
      .in("dump" / "article")
      .in(jsonBody[Draft])
      .errorOut(errorOutputsFor(400, 500))
      .out(jsonBody[Draft])
      .serverLogicPure { article =>
        writeService.insertDump(article)
      }
  }
}
