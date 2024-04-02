/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Dimensions of an image")
case class ImageDimensions(
    @description("The width of the image in pixels") width: Int,
    @description("The height of the image in pixels") height: Int
)

object ImageDimensions {
  implicit val encoder: Encoder[ImageDimensions] = deriveEncoder[ImageDimensions]
  implicit val decoder: Decoder[ImageDimensions] = deriveDecoder[ImageDimensions]
}
