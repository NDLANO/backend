/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Alt-text of an image")
case class ImageAltTextDTO(
    @description("The alternative text for the image") alttext: String,
    @description("ISO 639-1 code that represents the language used in the alternative text") language: String
)

object ImageAltTextDTO {
  implicit val encoder: Encoder[ImageAltTextDTO] = deriveEncoder
  implicit val decoder: Decoder[ImageAltTextDTO] = deriveDecoder
}
