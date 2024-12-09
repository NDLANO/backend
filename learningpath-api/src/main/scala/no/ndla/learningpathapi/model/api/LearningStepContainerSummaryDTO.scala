/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Summary of meta information for a learningstep including language and supported languages")
case class LearningStepContainerSummaryDTO(
    @description("The chosen search language") language: String,
    @description("The chosen search language") learningsteps: Seq[LearningStepSummaryV2DTO],
    @description("The chosen search language") supportedLanguages: Seq[String]
)

object LearningStepContainerSummaryDTO {
  implicit val encoder: Encoder[LearningStepContainerSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningStepContainerSummaryDTO] = deriveDecoder
}
