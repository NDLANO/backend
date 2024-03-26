/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.{Copyright, UpdateOrDelete}
import sttp.tapir.Schema.annotations.description

@description("Meta information for the image")
case class UpdateImageMetaInformation(
    @description("ISO 639-1 code that represents the language") language: String,
    @description("Title for the image") title: Option[String],
    @description("Alternative text for the image") alttext: UpdateOrDelete[String],
    @description("Describes the copyright information for the image") copyright: Option[Copyright],
    @description("Searchable tags for the image") tags: Option[Seq[String]],
    @description("Caption for the image") caption: Option[String],
    @description("Describes if the model has released use of the image") modelReleased: Option[String]
)

object UpdateImageMetaInformation {
  implicit val encoder: Encoder[UpdateImageMetaInformation] =
    UpdateOrDelete.filterMarkers(deriveEncoder[UpdateImageMetaInformation])
  implicit val decoder: Decoder[UpdateImageMetaInformation] = deriveDecoder
}
