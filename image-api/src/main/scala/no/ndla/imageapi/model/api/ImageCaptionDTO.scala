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
import sttp.tapir.Schema.annotations.description

@description("An image caption")
case class ImageCaptionDTO(
    @description("The caption for the image")
    caption: String,
    @description("ISO 639-1 code that represents the language used in the caption")
    language: String,
)

object ImageCaptionDTO {
  implicit def encoder: Encoder[ImageCaptionDTO] = deriveEncoder[ImageCaptionDTO]
  implicit def decoder: Decoder[ImageCaptionDTO] = deriveDecoder[ImageCaptionDTO]
}
