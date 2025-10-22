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
import sttp.tapir.Schema.annotations.{default, description, deprecated}

@description("Information about a new learningstep")
case class NewLearningStepV2DTO(
    @description("The titles of the learningstep")
    title: String,
    @description("The introduction of the learningstep")
    introduction: Option[String],
    @description("The descriptions of the learningstep")
    description: Option[String],
    @description("The chosen language")
    language: String,
    @description("The article id this learningstep points to")
    articleId: Option[Long],
    @description("The embed content for the learningstep")
    embedUrl: Option[EmbedUrlV2DTO],
    @description("Determines if the title of the step should be displayed in viewmode.")
    @default(false)
    showTitle: Boolean,
    @description("The type of the step")
    `type`: String,
    @description("Describes the copyright information for the learningstep")
    @deprecated
    license: Option[String],
    @description("Describes the copyright information for the learningstep")
    copyright: Option[CopyrightDTO],
)

object NewLearningStepV2DTO {
  implicit val encoder: Encoder[NewLearningStepV2DTO] = deriveEncoder
  implicit val decoder: Decoder[NewLearningStepV2DTO] = deriveDecoder
}
