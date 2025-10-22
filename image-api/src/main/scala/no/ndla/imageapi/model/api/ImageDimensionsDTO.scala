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
case class ImageDimensionsDTO(
    @description("The width of the image in pixels")
    width: Int,
    @description("The height of the image in pixels")
    height: Int,
)

object ImageDimensionsDTO {
  implicit val encoder: Encoder[ImageDimensionsDTO] = deriveEncoder[ImageDimensionsDTO]
  implicit val decoder: Decoder[ImageDimensionsDTO] = deriveDecoder[ImageDimensionsDTO]
}
