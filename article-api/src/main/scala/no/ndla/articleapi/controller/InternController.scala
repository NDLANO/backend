/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.Props
import no.ndla.articleapi.model.api.PartialPublishArticle
import no.ndla.articleapi.model.domain.DBArticle
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search.{ArticleIndexService, IndexService}
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.common.model.domain.article.Article
import no.ndla.language.Language
import no.ndla.network.tapir.auth.Permission.ARTICLE_API_WRITE
import org.json4s.Formats
import org.scalatra.{InternalServerError, NotFound, Ok}

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: ReadService
    with WriteService
    with ConverterService
    with ArticleRepository
    with IndexService
    with ArticleIndexService
    with ContentValidator
    with Props
    with DBArticle
    with NdlaController =>
  val internController: InternController

  class InternController extends NdlaController {
    protected implicit override val jsonFormats: Formats = Article.jsonEncoder

    post("/index") {
      val numShards    = intOrNone("numShards")
      implicit val ec  = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val articleIndex = Future { articleIndexService.indexDocuments(numShards) }

      Await.result(articleIndex, Duration(10, TimeUnit.MINUTES)) match {
        case Success(articleResult) =>
          val result =
            s"Completed indexing of ${articleResult.totalIndexed} articles ${articleResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        case Failure(articleFail) =>
          logger.warn(articleFail.getMessage, articleFail)
          InternalServerError(articleFail.getMessage)
      }
    }: Unit

    delete("/index") {
      implicit val ec         = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"

      val articleIndex = Future { articleIndexService.findAllIndexes(props.ArticleSearchIndex) }

      val deleteResults: Seq[Try[_]] = Await.result(articleIndex, Duration(10, TimeUnit.MINUTES)) match {
        case (Failure(articleFail)) => halt(status = 500, body = articleFail.getMessage)
        case (Success(articleIndexes)) => {
          articleIndexes.map(index => {
            logger.info(s"Deleting article index $index")
            articleIndexService.deleteIndexWithName(Option(index))
          })
        }
      }

      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }

    }: Unit

    get("/ids") {
      articleRepository.getAllIds
    }: Unit

    get("/id/:external_id") {
      val externalId = params("external_id")
      articleRepository.getIdFromExternalId(externalId) match {
        case Some(id) => id
        case None     => NotFound()
      }
    }: Unit

    get("/articles") {
      // Dumps Api articles
      val pageNo   = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      val lang     = paramOrDefault("language", Language.AllLanguages)
      val fallback = booleanOrDefault("fallback", default = false)

      readService.getArticlesByPage(pageNo, pageSize, lang, fallback)
    }: Unit

    get("/dump/article/?") {
      // Dumps Domain articles
      val pageNo   = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getArticleDomainDump(pageNo, pageSize)
    }: Unit

    get("/dump/article/:article_id") {
      val articleId = long("article_id")
      articleRepository.withId(articleId).map(_.article) match {
        case Some(value) => Ok(value)
        case None        => NotFound()
      }
    }: Unit

    post("/validate/article") {
      val importValidate = booleanOrDefault("import_validate", default = false)
      val article        = extract[Article](request.body)
      contentValidator.validateArticle(article, isImported = importValidate) match {
        case Success(_)  => article
        case Failure(ex) => errorHandler(ex)
      }
    }: Unit

    post("/article/:id") {
      doOrAccessDenied(ARTICLE_API_WRITE) {
        val externalIds         = paramAsListOfString("external-id")
        val useImportValidation = booleanOrDefault("use-import-validation", default = false)
        val useSoftValidation   = booleanOrDefault("use-soft-validation", default = false)
        val article             = extract[Article](request.body)
        val id                  = long("id")

        writeService.updateArticle(
          article.copy(id = Some(id)),
          externalIds,
          useImportValidation,
          useSoftValidation
        ) match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    delete("/article/:id/") {
      doOrAccessDenied(ARTICLE_API_WRITE) {
        val revision = intOrNone("revision")
        writeService.deleteArticle(long("id"), revision) match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    post("/article/:id/unpublish/") {
      doOrAccessDenied(ARTICLE_API_WRITE) {
        val revision = intOrNone("revision")
        writeService.unpublishArticle(long("id"), revision) match {
          case Success(a)  => a
          case Failure(ex) => errorHandler(ex)
        }
      }
    }: Unit

    patch("/partial-publish/:article_id") {
      doOrAccessDenied(ARTICLE_API_WRITE) {
        val articleId         = long("article_id")
        val partialUpdateBody = extract[PartialPublishArticle](request.body)
        val language          = paramOrDefault("language", Language.AllLanguages)
        val fallback          = booleanOrDefault("fallback", default = false)

        writeService.partialUpdate(articleId, partialUpdateBody, language, fallback) match {
          case Failure(ex)         => errorHandler(ex)
          case Success(apiArticle) => Ok(apiArticle)
        }
      }
    }: Unit
  }
}
