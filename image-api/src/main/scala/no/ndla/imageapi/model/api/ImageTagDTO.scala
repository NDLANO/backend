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

@description("An tag for an image")
case class ImageTagDTO(
    @description("The searchable tag.") tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag") language: String
)

object ImageTagDTO {
  implicit def encoder: Encoder[ImageTagDTO] = deriveEncoder[ImageTagDTO]
  implicit def decoder: Decoder[ImageTagDTO] = deriveDecoder[ImageTagDTO]
}
