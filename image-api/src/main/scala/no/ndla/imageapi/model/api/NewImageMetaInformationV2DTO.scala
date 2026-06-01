/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.CopyrightDTO
import no.ndla.common.model.domain.AiGenerated
import no.ndla.imageapi.model.domain.ModelReleasedStatus
import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information for the image")
case class NewImageMetaInformationV2DTO(
    @description("Title for the image") title: String,
    @description("Alternative text for the image") alttext: Option[String],
    @description("Describes the copyright information for the image") copyright: CopyrightDTO,
    @description("Searchable tags for the image") tags: Seq[String],
    @description("Caption for the image") caption: String,
    @description("ISO 639-1 code that represents the language used in the caption") language: String,
    @description("Describes if the model has released use of the image, defaults to 'no'") modelReleased: Option[ModelReleasedStatus],
    @description("Describes whether the image is AI generated") aiGenerated: Option[AiGenerated],
)

object NewImageMetaInformationV2DTO{
  implicit val encoder: Encoder[NewImageMetaInformationV2DTO] = deriveEncoder
  implicit val decoder: Decoder[NewImageMetaInformationV2DTO] = deriveDecoder
}
