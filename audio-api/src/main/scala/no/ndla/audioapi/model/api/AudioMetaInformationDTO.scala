/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.CopyrightDTO
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.DeriveHelpers

@description("Meta information about the audio object")
case class AudioMetaInformationDTO(
    @description("The unique id of this audio")
    id: Long,
    @description("The revision number of this audio")
    revision: Int,
    @description("The title of the audio file")
    title: TitleDTO,
    @description("The audio file for this language")
    audioFile: AudioDTO,
    @description("Copyright information for the audio files")
    copyright: CopyrightDTO,
    @description("Tags for this audio file")
    tags: TagDTO,
    @description("The languages available for this audio")
    supportedLanguages: Seq[String],
    @description("Type of audio. 'standard', or 'podcast'.")
    audioType: String,
    @description("Meta information about podcast, only applicable if audioType is 'podcast'.")
    podcastMeta: Option[PodcastMetaDTO],
    @description("Meta information about series if the audio is a podcast and a part of a series.")
    series: Option[SeriesDTO],
    @description("Manuscript for the audio")
    manuscript: Option[ManuscriptDTO],
    @description("The time of creation for the audio-file")
    created: NDLADate,
    @description("The time of last update for the audio-file")
    updated: NDLADate
)

object AudioMetaInformationDTO {
  implicit val encoder: Encoder[AudioMetaInformationDTO] = deriveEncoder
  implicit val decoder: Decoder[AudioMetaInformationDTO] = deriveDecoder

  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[AudioMetaInformationDTO] = DeriveHelpers.getSchema
}
