/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.api.CopyrightDTO
import sttp.tapir.Schema.annotations.description

@description("Meta information about the audio object")
case class NewAudioMetaInformationDTO(
    @description("The title of the audio file")
    title: String,
    @description("ISO 639-1 code that represents the language used in this resource")
    language: String,
    @description("Copyright information for the audio files")
    copyright: CopyrightDTO,
    @description("Tags for this audio file")
    tags: Seq[String],
    @description("Type of audio. 'standard', or 'podcast', defaults to 'standard'")
    audioType: Option[String],
    @description("Meta information about podcast, only applicable if audioType is 'podcast'.")
    podcastMeta: Option[NewPodcastMetaDTO],
    @description("Id of series if the audio is a podcast and a part of a series.")
    seriesId: Option[Long],
    @description("Manuscript for the audio")
    manuscript: Option[String],
)

object NewAudioMetaInformationDTO {
  implicit val encoder: Encoder[NewAudioMetaInformationDTO] = deriveEncoder
  implicit val decoder: Decoder[NewAudioMetaInformationDTO] = deriveDecoder
}
