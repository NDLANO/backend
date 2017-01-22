/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain.LanguageField
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.validation.ArticleValidator
import org.joda.time.DateTime


trait UpdateService {
  this: ArticleRepository with ConverterService with ArticleValidator =>
  val updateService: UpdateService

  class UpdateService {
    def newArticle(newArticle: api.NewArticle) = {
      val domainArticle = converterService.toDomainArticle(newArticle)
      articleValidator.validateArticle(domainArticle)
      converterService.toApiArticle(articleRepository.insert(domainArticle))
    }

    def mergeLanguageFields[A <: LanguageField](existing: Seq[A], updated: Seq[A]): Seq[A] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.value.isEmpty)
    }

    def mergTags(existing: Seq[domain.ArticleTag], updated: Seq[domain.ArticleTag]): Seq[domain.ArticleTag] = {
      val toKeep = existing.filterNot(item => updated.map(_.language).contains(item.language))
      (toKeep ++ updated).filterNot(_.tags.isEmpty)
    }

    def updateArticle(articleId: Long, updatedApiArticle: api.UpdatedArticle): Option[api.Article] = {
      articleRepository.withId(articleId) match {
        case None => None
        case Some(existing) =>
          val updatedArticle = converterService.toDomainArticle(updatedApiArticle)
          val toUpdate = existing.copy(
            title = mergeLanguageFields(existing.title, updatedArticle.title),
            content = mergeLanguageFields(existing.content, updatedArticle.content),
            copyright = updatedArticle.copyright,
            tags = mergTags(existing.tags, updatedArticle.tags),
            requiredLibraries = updatedArticle.requiredLibraries,
            visualElement = mergeLanguageFields(existing.visualElement, updatedArticle.visualElement),
            introduction = mergeLanguageFields(existing.introduction, updatedArticle.introduction),
            metaDescription = mergeLanguageFields(existing.metaDescription, updatedArticle.metaDescription),
            metaImageId = updatedArticle.metaImageId,
            updated = new DateTime().toDate,
            contentType = updatedArticle.contentType
          )
          articleValidator.validateArticle(toUpdate)
          articleRepository.update(toUpdate).map(converterService.toApiArticle)
      }
    }

  }
}
