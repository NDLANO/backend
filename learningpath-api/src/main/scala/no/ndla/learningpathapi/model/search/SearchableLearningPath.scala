/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.model.api.Copyright
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}

case class SearchableLearningPath(
    id: Long,
    titles: SearchableLanguageValues,
    descriptions: SearchableLanguageValues,
    coverPhotoUrl: Option[String],
    duration: Option[Int],
    status: String,
    verificationStatus: String,
    lastUpdated: NDLADate,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    learningsteps: Seq[SearchableLearningStep],
    copyright: Copyright,
    isBasedOn: Option[Long]
)

object SearchableLearningPath {
  implicit val encoder: Encoder[SearchableLearningPath] = deriveEncoder
  implicit val decoder: Decoder[SearchableLearningPath] = deriveDecoder
}
