/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.learningpathapi.model.api.CopyrightDTO

case class SearchableLearningPath(
    id: Long,
    titles: SearchableLanguageValues,
    descriptions: SearchableLanguageValues,
    introductions: SearchableLanguageValues,
    coverPhotoUrl: Option[String],
    duration: Option[Int],
    status: String,
    verificationStatus: String,
    created: NDLADate,
    lastUpdated: NDLADate,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    learningsteps: Seq[SearchableLearningStep],
    copyright: CopyrightDTO,
    isBasedOn: Option[Long],
    grepCodes: Seq[String]
)

object SearchableLearningPath {
  implicit val encoder: Encoder[SearchableLearningPath] = deriveEncoder
  implicit val decoder: Decoder[SearchableLearningPath] = deriveDecoder
}
