/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.model.{NotFoundException, api}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.search.ArticleIndexService
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.common.errors.ValidationException
import no.ndla.common.implicits.toTry
import no.ndla.common.model.domain.article.{Article, PartialPublishArticleDTO, PartialPublishArticlesBulkDTO}
import no.ndla.database.DBUtility
import no.ndla.language.Language
import no.ndla.network.clients.SearchApiClient
import scalikejdbc.DBSession

import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.util.{Failure, Success, Try}

class WriteService(using
    articleRepository: ArticleRepository,
    converterService: ConverterService,
    contentValidator: ContentValidator,
    articleIndexService: ArticleIndexService,
    searchApiClient: SearchApiClient,
    dBUtility: DBUtility,
) extends StrictLogging {
  private val executor: ExecutorService            = Executors.newSingleThreadExecutor
  implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(executor)

  private def performArticleValidation(
      article: Article,
      externalIds: List[String],
      useSoftValidation: Boolean,
      skipValidation: Boolean,
      useImportValidation: Boolean,
  )(using DBSession): Try[Article] = {
    val strictValidationResult =
      contentValidator.validateArticle(article, isImported = externalIds.nonEmpty || useImportValidation)

    val softOrStrictValidationResult =
      if (useSoftValidation && !skipValidation) {
        (
          strictValidationResult,
          contentValidator.softValidateArticle(article, isImported = useImportValidation),
        ) match {
          case (Failure(strictEx: ValidationException), Success(art)) =>
            val strictErrors = strictEx
              .errors
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

    (skipValidation, softOrStrictValidationResult) match {
      case (true, Failure(ex: ValidationException)) =>
        logger.warn(
          s"Article with id '${article.id.getOrElse(-1)}' was updated with validation skipped and failed with the following errors:\n${ex
              .errors
              .map(msg => {
                s"\t'${msg.field}' => '${msg.message}'"
              })
              .mkString("\n\t")}"
        )
        Success(article)
      case (_, result) => result
    }
  }

  def updateArticle(
      article: Article,
      externalIds: List[String],
      useImportValidation: Boolean,
      useSoftValidation: Boolean,
      skipValidation: Boolean,
  )(session: DBSession): Try[Article] = for {
    _ <- performArticleValidation(article, externalIds, useSoftValidation, skipValidation, useImportValidation)(using
      session
    )
    domainArticle <- articleRepository.updateArticleFromDraftApi(article, externalIds)(using session)
    _             <- articleIndexService.indexDocument(domainArticle)
    _             <- Try(searchApiClient.indexDocument("article", domainArticle, None))
  } yield domainArticle

  def partialUpdate(
      articleId: Long,
      partialArticle: PartialPublishArticleDTO,
      language: String,
      fallback: Boolean,
      isInBulk: Boolean,
  )(session: DBSession): Try[api.ArticleV2DTO] = for {
    maybeArticleRow <- articleRepository.withId(articleId)(using session)
    existingArticle <- maybeArticleRow
      .toArticle
      .toTry(NotFoundException(s"Could not find article with id '$articleId' to partial publish"))
    externalIds     <- articleRepository.getExternalIdsFromId(articleId)(using session)
    newArticle       = converterService.updateArticleFields(existingArticle, partialArticle)
    insertedArticle <- updateArticle(
      newArticle,
      externalIds,
      useImportValidation = false,
      useSoftValidation = true,
      skipValidation = isInBulk,
    )(session)
    converted <- converterService.toApiArticleV2(insertedArticle, language, externalIds, fallback)
  } yield converted

  def partialUpdateBulk(bulkInput: PartialPublishArticlesBulkDTO): Try[Unit] = {
    dBUtility
      .rollbackOnFailure { session =>
        bulkInput
          .idTo
          .toList
          .traverse { case (id, ppa) =>
            val updateResult = partialUpdate(id, ppa, Language.AllLanguages, fallback = true, isInBulk = true)(session)
              .map(_ => ())

            updateResult.recoverWith { case _: NotFoundException =>
              logger.warn(s"Article with id '$id' was not found when bulk partial publishing")
              Success(())
            }
          }
      }
      .map(_ => ())
  }

  def unpublishArticle(id: Long, revision: Option[Int]): Try[api.ArticleIdV2DTO] = {
    val updated = dBUtility.rollbackOnFailure { implicit session =>
      revision match {
        case Some(rev) => articleRepository.unpublish(id, rev)
        case None      => articleRepository.unpublishMaxRevision(id)
      }
    }

    updated
      .flatMap(articleIndexService.deleteDocument)
      .map(a => searchApiClient.deleteDocument(a, "article"))
      .map(api.ArticleIdV2DTO.apply)
  }

  def deleteArticle(id: Long, revision: Option[Int]): Try[api.ArticleIdV2DTO] = {
    val deleted = dBUtility.rollbackOnFailure { implicit session =>
      revision match {
        case Some(rev) => articleRepository.delete(id, rev)
        case None      => articleRepository.deleteMaxRevision(id)
      }
    }

    deleted
      .flatMap(articleIndexService.deleteDocument)
      .map(a => searchApiClient.deleteDocument(a, "article"))
      .map(api.ArticleIdV2DTO.apply)
  }
}
