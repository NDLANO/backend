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

@description("An tag for an image")
case class ImageTag(
    @description("The searchable tag.") tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag") language: String
)

object ImageTag {
  implicit def encoder: Encoder[ImageTag] = deriveEncoder[ImageTag]
  implicit def decoder: Decoder[ImageTag] = deriveDecoder[ImageTag]
}
