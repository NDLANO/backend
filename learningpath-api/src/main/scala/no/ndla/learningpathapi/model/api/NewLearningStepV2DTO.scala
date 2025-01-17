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

@description("Information about a new learningstep")
case class NewLearningStepV2DTO(
    @description("The titles of the learningstep") title: String,
    @description("The introduction of the learningstep") introduction: Option[String],
    @description("The descriptions of the learningstep") description: Option[String],
    @description("The chosen language") language: String,
    @description("The embed content for the learningstep") embedUrl: Option[EmbedUrlV2DTO],
    @description(
      "Determines if the title of the step should be displayed in viewmode. Default is false."
    ) showTitle: Option[Boolean],
    @description("The type of the step") `type`: String,
    @description("Describes the copyright information for the learningstep") license: Option[String]
)

object NewLearningStepV2DTO {
  implicit val encoder: Encoder[NewLearningStepV2DTO] = deriveEncoder
  implicit val decoder: Decoder[NewLearningStepV2DTO] = deriveDecoder
}
