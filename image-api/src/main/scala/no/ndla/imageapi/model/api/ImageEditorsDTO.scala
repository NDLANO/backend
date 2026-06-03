/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("A list of image editors")
case class ImageEditorsDTO(
    @description("The user ids of the editors")
    ids: Option[Seq[String]]
)

object ImageEditorsDTO {
  implicit val encoder: Encoder[ImageEditorsDTO] = deriveEncoder[ImageEditorsDTO]
  implicit val decoder: Decoder[ImageEditorsDTO] = deriveDecoder[ImageEditorsDTO]
}
