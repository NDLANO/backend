/*
 * Part of NDLA audio-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("The result of a transcription job")
case class TranscriptionResultDTO(
    @description("The status of the transcription job") status: String,
    @description("The transcription of the audio") transcription: Option[String]
)
object TranscriptionResultDTO {
  implicit val encoder: Encoder[TranscriptionResultDTO] = deriveEncoder
  implicit val decoder: Decoder[TranscriptionResultDTO] = deriveDecoder
}
