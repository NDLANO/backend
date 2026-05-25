/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.integration

import cats.implicits.*
import no.ndla.common.model.api.{Delete, Missing, RelatedContentLinkDTO, UpdateWith}
import no.ndla.common.model.domain.Availability
import no.ndla.common.model.domain.article.{ArticleMetaDescriptionDTO, ArticleTagDTO, PartialPublishArticleDTO}
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.draftapi.model.api.ContentIdDTO
import no.ndla.network.tapir.auth.TokenUser

import scala.util.Try
import common.getNextRevision

trait ArticleApiClient {
  def partialPublishArticle(id: Long, article: PartialPublishArticleDTO, user: TokenUser): Try[Long]
  def updateArticle(id: Long, draft: Draft, useSoftValidation: Boolean, user: TokenUser): Try[Draft]
  def unpublishArticle(article: Draft, user: TokenUser): Try[Draft]
  def deleteArticle(id: Long, user: TokenUser): Try[ContentIdDTO]
  def validateArticle(
      article: common.article.Article,
      importValidate: Boolean,
      user: Option[TokenUser],
  ): Try[common.article.Article]
  def bulkPartialPublishArticles(ids: Map[Long, PartialPublishArticleDTO], user: TokenUser): Try[Unit]
}

extension (self: PartialPublishArticleDTO) {
  def withLicense(license: Option[String]): PartialPublishArticleDTO              = self.copy(license = license)
  def withGrepCodes(grepCodes: Seq[String]): PartialPublishArticleDTO             = self.copy(grepCodes = grepCodes.some)
  def withTags(tags: Seq[common.Tag], language: String): PartialPublishArticleDTO =
    self.copy(tags = tags.find(t => t.language == language).toSeq.map(t => ArticleTagDTO(t.tags, t.language)).some)
  def withTags(tags: Seq[common.Tag]): PartialPublishArticleDTO =
    self.copy(tags = tags.map(t => ArticleTagDTO(t.tags, t.language)).some)
  def withRelatedContent(relatedContent: Seq[common.RelatedContent]): PartialPublishArticleDTO = {
    val api = relatedContent.map { rc =>
      rc.leftMap { rcl =>
        RelatedContentLinkDTO(rcl.title, rcl.url)
      }
    }
    self.copy(relatedContent = api.some)
  }

  def withMetaDescription(meta: Seq[common.Description], language: String): PartialPublishArticleDTO =
    self.copy(metaDescription =
      meta.find(m => m.language == language).map(m => ArticleMetaDescriptionDTO(m.content, m.language)).toSeq.some
    )
  def withMetaDescription(meta: Seq[common.Description]): PartialPublishArticleDTO = {
    val api = meta.map(m => ArticleMetaDescriptionDTO(m.content, m.language))
    self.copy(metaDescription = api.some)
  }
  def withAvailability(availability: Availability): PartialPublishArticleDTO =
    self.copy(availability = availability.some)
  def withEarliestRevisionDate(revisionMeta: Seq[common.RevisionMeta]): PartialPublishArticleDTO = {
    val earliestRevisionDate = revisionMeta.getNextRevision.map(_.revisionDate)
    val newRev               = earliestRevisionDate match {
      case Some(value) => UpdateWith(value)
      case None        => Delete
    }
    self.copy(revisionDate = newRev)
  }
  def withRevised(revised: NDLADate): PartialPublishArticleDTO = self.copy(revised = revised.some)
}

object PartialPublishArticle {
  def empty(): PartialPublishArticleDTO = PartialPublishArticleDTO(None, None, None, None, None, None, Missing, None)
}
