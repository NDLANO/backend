/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.Copyright
import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information for the image")
case class NewImageMetaInformationV2(
    @description("Title for the image") title: String,
    @description("Alternative text for the image") alttext: Option[String],
    @description("Describes the copyright information for the image") copyright: Copyright,
    @description("Searchable tags for the image") tags: Seq[String],
    @description("Caption for the image") caption: String,
    @description("ISO 639-1 code that represents the language used in the caption") language: String,
    @description("Describes if the model has released use of the image, allowed values are 'not-set', 'yes', 'no', and 'not-applicable', defaults to 'no'") modelReleased: Option[String]
)

object NewImageMetaInformationV2{
  implicit val encoder: Encoder[NewImageMetaInformationV2] = deriveEncoder
  implicit val decoder: Decoder[NewImageMetaInformationV2] = deriveDecoder
}
