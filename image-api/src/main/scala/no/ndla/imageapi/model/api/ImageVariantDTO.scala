/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.imageapi.model.domain.ImageVariantSize
import sttp.tapir.Schema.annotations.description

case class ImageVariantDTO(
    @description("The named size of this image variant")
    size: ImageVariantSize,
    @description("The full URL to where the image variant can be downloaded")
    variantUrl: String,
)

object ImageVariantDTO {
  implicit val encoder: Encoder[ImageVariantDTO] = deriveEncoder[ImageVariantDTO]
  implicit val decoder: Decoder[ImageVariantDTO] = deriveDecoder[ImageVariantDTO]
}
