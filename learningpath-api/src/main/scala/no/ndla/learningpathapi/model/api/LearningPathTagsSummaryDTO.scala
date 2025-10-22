/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class LearningPathTagsSummaryDTO(
    @description("The chosen language. Default is 'nb'")
    language: String,
    @description("The supported languages for these tags")
    supportedLanguages: Seq[String],
    @description("The searchable tags. Must be plain text")
    tags: Seq[String],
)

object LearningPathTagsSummaryDTO {
  implicit val encoder: Encoder[LearningPathTagsSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningPathTagsSummaryDTO] = deriveDecoder
}
