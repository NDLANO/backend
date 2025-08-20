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
import no.ndla.common.model.api.UpdateOrDelete
import sttp.tapir.Schema.annotations.{description, deprecated}

@description("Information about a new learningstep")
case class UpdatedLearningStepV2DTO(
    @description("The revision number for this learningstep")
    revision: Int,
    @description("The title of the learningstep")
    title: Option[String],
    @description("The introduction of the learningstep")
    introduction: UpdateOrDelete[String],
    @description("The chosen language")
    language: String,
    @description("The description of the learningstep")
    description: UpdateOrDelete[String],
    @description("The embed content for the learningstep")
    embedUrl: UpdateOrDelete[EmbedUrlV2DTO],
    @description("The article id this learningstep points to")
    articleId: UpdateOrDelete[Long],
    @description("Determines if the title of the step should be displayed in viewmode")
    showTitle: Option[Boolean],
    @description("The type of the step")
    `type`: Option[String],
    @description("Describes the copyright information for the learningstep")
    @deprecated
    license: Option[String],
    @description("Describes the copyright information for the learningstep")
    copyright: UpdateOrDelete[CopyrightDTO]
)

object UpdatedLearningStepV2DTO {
  implicit val encoder: Encoder[UpdatedLearningStepV2DTO] = deriveEncoder
  implicit val decoder: Decoder[UpdatedLearningStepV2DTO] = deriveDecoder
}
