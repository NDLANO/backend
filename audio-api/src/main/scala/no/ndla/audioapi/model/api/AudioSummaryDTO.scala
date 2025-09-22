/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import sttp.tapir.Schema
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.DeriveHelpers

@description("Short summary of information about the audio")
case class AudioSummaryDTO(
    @description("The unique id of the audio")
    id: Long,
    @description("The title of the audio")
    title: TitleDTO,
    @description("The audioType. Possible values standard and podcast")
    audioType: String,
    @description("The full url to where the complete information about the audio can be found")
    url: String,
    @description("Describes the license of the audio")
    license: String,
    @description("A list of available languages for this audio")
    supportedLanguages: Seq[String],
    @description("A manuscript for the audio")
    manuscript: Option[ManuscriptDTO],
    @description("Meta information about podcast, only applicable if audioType is 'podcast'.")
    podcastMeta: Option[PodcastMetaDTO],
    @description("Series that the audio is part of")
    series: Option[SeriesSummaryDTO],
    @description("The time and date of last update")
    lastUpdated: NDLADate
)

object AudioSummaryDTO {
  implicit val encoder: Encoder[AudioSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[AudioSummaryDTO] = deriveDecoder

  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[AudioSummaryDTO] = DeriveHelpers.getSchema
}
