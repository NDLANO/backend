/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class TitleDTO(
    @description("The title of the audio file") title: String,
    @description("ISO 639-1 code that represents the language used in the title") language: String
)

object TitleDTO {
  implicit val encoder: Encoder[TitleDTO] = deriveEncoder
  implicit val decoder: Decoder[TitleDTO] = deriveDecoder
}
