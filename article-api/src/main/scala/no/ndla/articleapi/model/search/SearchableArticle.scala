/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ArticleMetaImage
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    metaDescription: SearchableLanguageValues,
    metaImage: Seq[ArticleMetaImage],
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    license: String,
    authors: Seq[String],
    articleType: String,
    defaultTitle: Option[String],
    grepCodes: Option[Seq[String]],
    availability: String
)

object SearchableArticle {
  implicit val encoder: Encoder[SearchableArticle] = deriveEncoder
  implicit val decoder: Decoder[SearchableArticle] = deriveDecoder
}
