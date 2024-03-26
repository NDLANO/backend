/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Title of the image")
case class ImageTitle(
    @description("The freetext title of the image") title: String,
    @description("ISO 639-1 code that represents the language used in title") language: String
)

object ImageTitle {
  implicit val encoder: Encoder[ImageTitle] = deriveEncoder[ImageTitle]
  implicit val decoder: Decoder[ImageTitle] = deriveDecoder[ImageTitle]
}
