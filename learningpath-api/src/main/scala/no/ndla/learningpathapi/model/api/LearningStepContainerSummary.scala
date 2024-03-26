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
case class LearningStepContainerSummary(
    @description("The chosen search language") language: String,
    @description("The chosen search language") learningsteps: Seq[LearningStepSummaryV2],
    @description("The chosen search language") supportedLanguages: Seq[String]
)

object LearningStepContainerSummary {
  implicit val encoder: Encoder[LearningStepContainerSummary] = deriveEncoder
  implicit val decoder: Decoder[LearningStepContainerSummary] = deriveDecoder
}
