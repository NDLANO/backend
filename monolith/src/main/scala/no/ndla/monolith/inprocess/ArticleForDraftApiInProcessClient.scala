/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.common.model.domain as common
import no.ndla.common.model.domain.article.PartialPublishArticleDTO
import no.ndla.common.model.domain.draft.Draft
import no.ndla.draftapi.integration.ArticleApiClient
import no.ndla.draftapi.model.api.ContentIdDTO
import no.ndla.language.Language
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Try

/** In-process implementation of draft-api's `ArticleApiClient` trait. Instead of POSTing JSON to article-api's intern
  * endpoints over loopback HTTP, this delegates directly to article-api's services running in the same JVM.
  *
  * Both component registries are passed by-name so that monolith wiring can construct this client before either per-app
  * `ComponentRegistry` has fully initialised — avoiding init-order cycles.
  */
class ArticleForDraftApiInProcessClient(
    articleApiCr: => no.ndla.articleapi.ComponentRegistry,
    draftApiCr: => no.ndla.draftapi.ComponentRegistry,
) extends ArticleApiClient {

  override def partialPublishArticle(id: Long, article: PartialPublishArticleDTO, user: TokenUser): Try[Long] = {
    // Mirrors InternController.partialPublishArticle: defaults match the endpoint's query-param defaults.
    articleApiCr
      .dbUtility
      .rollbackOnFailure { session =>
        articleApiCr
          .writeService
          .partialUpdate(id, article, Language.AllLanguages, fallback = false, isInBulk = false)(session)
      }
      .map(_.id)
  }

  override def updateArticle(id: Long, draft: Draft, useSoftValidation: Boolean, user: TokenUser): Try[Draft] = {
    // Same conversion the HTTP client performs before POSTing; keeps article-api ignorant of draft-api types.
    for {
      converted <- draftApiCr.converterService.toArticleApiArticle(draft, false)
      _         <- articleApiCr
        .dbUtility
        .rollbackOnFailure { session =>
          articleApiCr
            .writeService
            .updateArticle(
              converted.copy(id = Some(id)),
              useImportValidation = false,
              useSoftValidation = useSoftValidation,
              skipValidation = false,
            )(session)
        }
    } yield draft
  }

  override def unpublishArticle(article: Draft, user: TokenUser): Try[Draft] = {
    val id = article.id.get
    articleApiCr.writeService.unpublishArticle(id, revision = None).map(_ => article)
  }

  override def deleteArticle(id: Long, user: TokenUser): Try[ContentIdDTO] = {
    articleApiCr.writeService.deleteArticle(id, revision = None).map(res => ContentIdDTO(res.id))
  }

  override def validateArticle(
      article: common.article.Article,
      importValidate: Boolean,
      user: Option[TokenUser],
  ): Try[common.article.Article] = {
    articleApiCr
      .dbUtility
      .readOnly { implicit session =>
        articleApiCr.contentValidator.validateArticle(article, isImported = importValidate)
      }
  }

  override def bulkPartialPublishArticles(ids: Map[Long, PartialPublishArticleDTO], user: TokenUser): Try[Unit] = {
    val bulk = no.ndla.common.model.domain.article.PartialPublishArticlesBulkDTO(idTo = ids)
    articleApiCr.writeService.partialUpdateBulk(bulk)
  }
}
