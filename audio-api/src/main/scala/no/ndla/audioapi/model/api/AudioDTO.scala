/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

@description("Url and size information about the audio")
case class AudioDTO(
    @description("The path to where the audio is located") url: String,
    @description("The mime type of the audio file") mimeType: String,
    @description("The size of the audio file") fileSize: Long,
    @description("The current language for this audio") language: String
)

object AudioDTO {
  implicit val encoder: Encoder[AudioDTO] = deriveEncoder
  implicit val decoder: Decoder[AudioDTO] = deriveDecoder
}
