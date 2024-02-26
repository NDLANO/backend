/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Status information about a learningpath")
case class LearningStepStatus(
    @description("The status of the learningstep") status: String
)

object LearningStepStatus {
  implicit val encoder: Encoder[LearningStepStatus] = deriveEncoder
  implicit val decoder: Decoder[LearningStepStatus] = deriveDecoder
}
