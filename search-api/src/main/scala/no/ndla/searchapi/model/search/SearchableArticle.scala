/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.*
import no.ndla.common.model.domain.ArticleMetaImage
import no.ndla.common.model.domain.article.Article
import no.ndla.search.model.domain.EmbedValues

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    license: String,
    status: String,
    authors: List[String],
    articleType: String,
    metaImage: List[ArticleMetaImage],
    defaultTitle: Option[String],
    supportedLanguages: List[String],
    context: Option[SearchableTaxonomyContext],
    contexts: List[SearchableTaxonomyContext],
    contextids: List[String],
    grepContexts: List[SearchableGrepContext],
    traits: List[ArticleTrait],
    embedAttributes: SearchableLanguageList,
    embedResourcesAndIds: List[EmbedValues],
    availability: String,
    learningResourceType: LearningResourceType,
    typeName: List[String],
    domainObject: Article,
)

object SearchableArticle {
  implicit val encoder: Encoder[SearchableArticle] = deriveEncoder
  implicit val decoder: Decoder[SearchableArticle] = deriveDecoder
}
