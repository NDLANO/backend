/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Meta information about podcast audio")
case class CoverPhotoDTO(
    @description("Id for the coverPhoto in image-api") id: String,
    @description("Url to the coverPhoto") url: String,
    @description("Alttext for the coverPhoto") altText: String
)

object CoverPhotoDTO {
  implicit val encoder: io.circe.Encoder[CoverPhotoDTO] = io.circe.generic.semiauto.deriveEncoder
  implicit val decoder: io.circe.Decoder[CoverPhotoDTO] = io.circe.generic.semiauto.deriveDecoder
}
