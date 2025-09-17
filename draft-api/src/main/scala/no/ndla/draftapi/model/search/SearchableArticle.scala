/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.ArticleTrait
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableArticle(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues,
    visualElement: SearchableLanguageValues,
    introduction: SearchableLanguageValues,
    tags: SearchableLanguageList,
    lastUpdated: NDLADate,
    license: Option[String],
    authors: Seq[String],
    articleType: String,
    notes: Seq[String],
    defaultTitle: Option[String],
    users: Seq[String],
    previousNotes: Seq[String],
    grepCodes: Seq[String],
    status: SearchableStatus,
    traits: List[ArticleTrait]
)

object SearchableArticle {
  implicit val encoder: Encoder[SearchableArticle] = deriveEncoder
  implicit val decoder: Decoder[SearchableArticle] = deriveDecoder
}
