/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema.annotations.description
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class ManuscriptDTO(
    @description("The manuscript of the audio file") manuscript: String,
    @description("ISO 639-1 code that represents the language used in the manuscript") language: String
)

object ManuscriptDTO {
  implicit val encoder: Encoder[ManuscriptDTO] = deriveEncoder
  implicit val decoder: Decoder[ManuscriptDTO] = deriveDecoder

}
