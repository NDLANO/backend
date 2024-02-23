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

@description("An image caption")
case class ImageCaption(
    @description("The caption for the image") caption: String,
    @description("ISO 639-1 code that represents the language used in the caption") language: String
)

object ImageCaption {
  implicit def encoder: Encoder[ImageCaption] = deriveEncoder[ImageCaption]
  implicit def decoder: Decoder[ImageCaption] = deriveDecoder[ImageCaption]
}
