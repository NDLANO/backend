/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto.*
import no.ndla.articleapi.Props
import no.ndla.articleapi.controller.ArticleErrorHelpers.ArticleGoneException
import no.ndla.articleapi.model.NotFoundException
import no.ndla.articleapi.model.api.*
import no.ndla.articleapi.model.domain.ArticleIds
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.*
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.common.implicits.toTry
import no.ndla.common.model.api.CommaSeparatedList.*
import no.ndla.common.model.domain.article.{Article, PartialPublishArticleDTO, PartialPublishArticlesBulkDTO}
import no.ndla.database.DBUtility
import no.ndla.language.Language
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.TapirUtil.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.ARTICLE_API_WRITE
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, TapirController}
import no.ndla.network.clients.MyNDLAApiClient
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.*
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class InternController(using
    readService: ReadService,
    writeService: WriteService,
    articleRepository: ArticleRepository,
    articleIndexService: ArticleIndexService,
    contentValidator: ContentValidator,
    dBUtility: DBUtility,
    props: Props,
    errorHandling: ErrorHandling,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: MyNDLAApiClient,
) extends TapirController
    with StrictLogging {
  override val prefix: EndpointInput[Unit] = "intern"
  override val enableSwagger               = false
  private val stringInternalServerError    = statusCode(StatusCode.InternalServerError).and(stringBody)

  def index: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("index")
    .in(query[Option[Int]]("numShards"))
    .out(stringBody)
    .errorOut(stringInternalServerError)
    .serverLogicPure(numShards => {
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val articleIndex = Future {
        articleIndexService.indexDocuments(numShards)
      }

      Await.result(articleIndex, Duration(10, TimeUnit.MINUTES)) match {
        case Success(articleResult) =>
          val result =
            s"Completed indexing of ${articleResult.totalIndexed} articles in ${articleResult.millisUsed} ms."
          logger.info(result)
          result.asRight
        case Failure(articleFail) =>
          logger.warn(articleFail.getMessage, articleFail)
          articleFail.getMessage.asLeft
      }
    })

  def deleteIndex: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .in("index")
    .out(stringBody)
    .errorOut(stringInternalServerError)
    .serverLogicPure { _ =>
      implicit val ec: ExecutionContextExecutorService =
        ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      def pluralIndex(n: Int) =
        if (n == 1) "1 index"
        else s"$n indexes"

      val articleIndex = Future {
        articleIndexService.findAllIndexes(props.ArticleSearchIndex)
      }

      Await.result(articleIndex, Duration(10, TimeUnit.MINUTES)) match {
        case Failure(articleFail)    => Left(articleFail.getMessage)
        case Success(articleIndexes) =>
          val deleteResults = articleIndexes.map(index => {
            logger.info(s"Deleting article index $index")
            articleIndexService.deleteIndexWithName(Option(index))
          })
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
    }

  def getIds: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("ids")
    .out(jsonBody[Seq[ArticleIds]])
    .errorOut(errorOutputsFor(500))
    .serverLogicPure(_ => dBUtility.readOnly(implicit session => articleRepository.getAllIds))

  def getByExternalId: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("id")
    .in(path[String]("external_id"))
    .out(jsonBody[Long])
    .errorOut(errorOutputsFor(404, 500))
    .serverLogicPure { externalId =>
      dBUtility.readOnly { implicit session =>
        articleRepository.getIdFromExternalId(externalId)
      }
    }

  def dumpApiArticles: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("articles")
    .in(query[Int]("page").default(1))
    .in(query[Int]("page-size").default(250))
    .in(query[String]("language").default(Language.AllLanguages))
    .in(query[Boolean]("fallback").default(false))
    .out(jsonBody[ArticleDumpDTO])
    .errorOut(errorOutputsFor(500))
    .serverLogicPure { case (pageNo, pageSize, language, fallback) =>
      readService.getArticlesByPage(pageNo, pageSize, language, fallback)
    }

  def dumpDomainArticles: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("dump" / "article")
    .in(query[Int]("page").default(1))
    .in(query[Int]("page-size").default(250))
    .out(jsonBody[ArticleDomainDumpDTO])
    .errorOut(errorOutputsFor(500))
    .serverLogicPure { case (pageNo, pageSize) =>
      readService.getArticleDomainDump(pageNo, pageSize)
    }

  def dumpSingleDomainArticle: ServerEndpoint[Any, Eff] = endpoint
    .get
    .in("dump" / "article" / path[Long]("article_id"))
    .out(jsonBody[Article])
    .errorOut(errorOutputsFor(404, 410, 500))
    .serverLogicPure { articleId =>
      dBUtility.readOnly { implicit session =>
        articleRepository
          .withId(articleId)
          .flatMap(_.toTry(NotFoundException(s"Article with ID $articleId was not found")))
          .flatMap(_.article.toTry(ArticleGoneException(s"Article data for ID $articleId was missing")))
      }
    }

  def validateArticle: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("validate" / "article")
    .in(query[Boolean]("import_validate").default(false))
    .in(jsonBody[Article])
    .out(jsonBody[Article])
    .errorOut(errorOutputsFor(400))
    .serverLogicPure { case (importValidate, article) =>
      dBUtility.readOnly { implicit session =>
        contentValidator.validateArticle(article, isImported = importValidate)
      }
    }

  def updateArticle: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("article" / path[Long]("id"))
    .in(listQuery[String]("external-id"))
    .in(query[Boolean]("use-import-validation").default(false))
    .in(query[Boolean]("use-soft-validation").default(false))
    .in(jsonBody[Article])
    .errorOut(errorOutputsFor(401, 403, 404))
    .out(jsonBody[Article])
    .requirePermission(ARTICLE_API_WRITE)
    .serverLogicPure { _ => params =>
      dBUtility.rollbackOnFailure {
        val (id, externalIds, useImportValidation, useSoftValidation, article) = params
        writeService.updateArticle(
          article.copy(id = Some(id)),
          externalIds.values.filterNot(_.isEmpty),
          useImportValidation,
          useSoftValidation,
          skipValidation = false,
        )
      }
    }

  def deleteArticle: ServerEndpoint[Any, Eff] = endpoint
    .delete
    .in("article" / path[Long]("id"))
    .in(query[Option[Int]]("revision"))
    .errorOut(errorOutputsFor(401, 403, 404))
    .out(jsonBody[ArticleIdV2DTO])
    .requirePermission(ARTICLE_API_WRITE)
    .serverLogicPure { _ => params =>
      val (id, revision) = params
      writeService.deleteArticle(id, revision)
    }

  def unpublishArticle: ServerEndpoint[Any, Eff] = endpoint
    .post
    .in("article" / path[Long]("id") / "unpublish")
    .in(query[Option[Int]]("revision"))
    .errorOut(errorOutputsFor(401, 403, 404))
    .out(jsonBody[ArticleIdV2DTO])
    .requirePermission(ARTICLE_API_WRITE)
    .serverLogicPure { _ => params =>
      val (id, revision) = params
      writeService.unpublishArticle(id, revision)
    }

  def partialPublishArticle: ServerEndpoint[Any, Eff] = endpoint
    .patch
    .in("partial-publish" / path[Long]("article_id"))
    .in(jsonBody[PartialPublishArticleDTO])
    .in(query[String]("language").default(Language.AllLanguages))
    .in(query[Boolean]("fallback").default(false))
    .errorOut(errorOutputsFor(401, 403, 404))
    .out(jsonBody[ArticleV2DTO])
    .requirePermission(ARTICLE_API_WRITE)
    .serverLogicPure { _ => params =>
      dBUtility.rollbackOnFailure {
        val (articleId, partialUpdateBody, language, fallback) = params
        writeService.partialUpdate(articleId, partialUpdateBody, language, fallback, isInBulk = false)
      }
    }

  def partialPublishMultiple: ServerEndpoint[Any, Eff] = endpoint
    .patch
    .in("partial-publish")
    .in(jsonBody[PartialPublishArticlesBulkDTO])
    .errorOut(errorOutputsFor(401, 403, 404))
    .out(emptyOutput)
    .requirePermission(ARTICLE_API_WRITE)
    .serverLogicPure { _ => input =>
      writeService.partialUpdateBulk(input)
    }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    index,
    deleteIndex,
    getIds,
    getByExternalId,
    dumpApiArticles,
    dumpDomainArticles,
    dumpSingleDomainArticle,
    validateArticle,
    updateArticle,
    deleteArticle,
    unpublishArticle,
    partialPublishArticle,
    partialPublishMultiple,
  )
}
