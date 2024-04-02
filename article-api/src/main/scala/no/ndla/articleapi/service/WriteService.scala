/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.integration.SearchApiClient
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.{NotFoundException, PartialPublishArticle}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.domain.article.Article

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ArticleRepository
    with ConverterService
    with ContentValidator
    with ArticleIndexService
    with ReadService
    with SearchApiClient =>
  val writeService: WriteService

  class WriteService extends StrictLogging {

    def updateArticle(
        article: Article,
        externalIds: List[String],
        useImportValidation: Boolean,
        useSoftValidation: Boolean
    ): Try[Article] = {

      val strictValidationResult = contentValidator.validateArticle(
        article,
        isImported = externalIds.nonEmpty || useImportValidation
      )

      val validationResult =
        if (useSoftValidation) {
          (
            strictValidationResult,
            contentValidator.softValidateArticle(article, isImported = useImportValidation)
          ) match {
            case (Failure(strictEx: ValidationException), Success(art)) =>
              val strictErrors = strictEx.errors
                .map(msg => {
                  s"\t'${msg.field}' => '${msg.message}'"
                })
                .mkString("\n\t")

              logger.warn(
                s"Article with id '${art.id.getOrElse(-1)}' was updated with soft validation while strict validation failed with the following errors:\n$strictErrors"
              )
              Success(art)
            case (_, Success(art)) => Success(art)
            case (_, Failure(ex))  => Failure(ex)
          }
        } else strictValidationResult

      for {
        _             <- validationResult
        domainArticle <- articleRepository.updateArticleFromDraftApi(article, externalIds)
        _             <- articleIndexService.indexDocument(domainArticle)
        _             <- Try(searchApiClient.indexArticle(domainArticle))
      } yield domainArticle
    }

    def partialUpdate(
        articleId: Long,
        partialArticle: PartialPublishArticle,
        language: String,
        fallback: Boolean
    ): Try[api.ArticleV2] = {
      articleRepository.withId(articleId).toArticle match {
        case None => Failure(NotFoundException(s"Could not find article with id '$articleId' to partial publish"))
        case Some(existingArticle) =>
          val newArticle  = converterService.updateArticleFields(existingArticle, partialArticle)
          val externalIds = articleRepository.getExternalIdsFromId(articleId)

          updateArticle(
            newArticle,
            externalIds,
            useImportValidation = false,
            useSoftValidation = false
          ).flatMap(insertedArticle => converterService.toApiArticleV2(insertedArticle, language, fallback))
      }
    }

    def unpublishArticle(id: Long, revision: Option[Int]): Try[api.ArticleIdV2] = {
      val updated = revision match {
        case Some(rev) => articleRepository.unpublish(id, rev)
        case None      => articleRepository.unpublishMaxRevision(id)
      }

      updated
        .flatMap(articleIndexService.deleteDocument)
        .map(searchApiClient.deleteArticle)
        .map(api.ArticleIdV2)
    }

    def deleteArticle(id: Long, revision: Option[Int]): Try[api.ArticleIdV2] = {
      val deleted = revision match {
        case Some(rev) => articleRepository.delete(id, rev)
        case None      => articleRepository.deleteMaxRevision(id)
      }

      deleted
        .flatMap(articleIndexService.deleteDocument)
        .map(searchApiClient.deleteArticle)
        .map(api.ArticleIdV2)
    }

  }
}
