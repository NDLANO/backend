/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ArticleMetaImage
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    license: String,
    authors: List[String],
    articleType: String,
    metaImage: List[ArticleMetaImage],
    defaultTitle: Option[String],
    supportedLanguages: List[String],
    contexts: List[SearchableTaxonomyContext],
    grepContexts: List[SearchableGrepContext],
    traits: List[String],
    embedAttributes: SearchableLanguageList,
    embedResourcesAndIds: List[EmbedValues],
    availability: String
)

object SearchableArticle {
  implicit val encoder: Encoder[SearchableArticle] = deriveEncoder
  implicit val decoder: Decoder[SearchableArticle] = deriveDecoder
}
