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

@description("Status information about a learningpath")
case class LearningStepStatusDTO(
    @description("The status of the learningstep")
    status: String
)

object LearningStepStatusDTO {
  implicit val encoder: Encoder[LearningStepStatusDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningStepStatusDTO] = deriveDecoder
}
