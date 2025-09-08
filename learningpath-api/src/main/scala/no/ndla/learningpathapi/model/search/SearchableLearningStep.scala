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
import no.ndla.common.model.api.search.SearchableLanguageValues

case class SearchableLearningStep(
    stepType: String,
    embedUrl: List[String],
    articleId: Option[Long],
    status: String,
    titles: SearchableLanguageValues,
    descriptions: SearchableLanguageValues
)

object SearchableLearningStep {
  implicit val encoder: Encoder[SearchableLearningStep] = deriveEncoder
  implicit val decoder: Decoder[SearchableLearningStep] = deriveDecoder
}
