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
case class UpdatedLearningStepV2(
    @description("The revision number for this learningstep") revision: Int,
    @description("The title of the learningstep") title: Option[String],
    @description("The chosen language") language: String,
    @description("The description of the learningstep") description: Option[String],
    @description("The embed content for the learningstep") embedUrl: Option[EmbedUrlV2],
    @description("Determines if the title of the step should be displayed in viewmode") showTitle: Option[Boolean],
    @description("The type of the step") `type`: Option[String],
    @description("Describes the copyright information for the learningstep") license: Option[String]
)

object UpdatedLearningStepV2 {
  implicit val encoder: Encoder[UpdatedLearningStepV2] = deriveEncoder
  implicit val decoder: Decoder[UpdatedLearningStepV2] = deriveDecoder
}
