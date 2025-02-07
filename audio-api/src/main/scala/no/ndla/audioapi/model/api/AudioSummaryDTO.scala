/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import com.scalatsi._
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

// format: off
@description("Short summary of information about the audio")
case class AudioSummaryDTO(
                            @description("The unique id of the audio") id: Long,
                            @description("The title of the audio") title: TitleDTO,
                            @description("The audioType. Possible values standard and podcast") audioType: String,
                            @description("The full url to where the complete information about the audio can be found") url: String,
                            @description("Describes the license of the audio") license: String,
                            @description("A list of available languages for this audio") supportedLanguages: Seq[String],
                            @description("A manuscript for the audio") manuscript: Option[ManuscriptDTO],
                            @description("Meta information about podcast, only applicable if audioType is 'podcast'.") podcastMeta: Option[PodcastMetaDTO],
                            @description("Series that the audio is part of") series: Option[SeriesSummaryDTO],
                            @description("The time and date of last update") lastUpdated: NDLADate
)
// format: on

object AudioSummaryDTO {
  implicit val AudioSummaryTSI: TSIType[AudioSummaryDTO] = {
    @unused
    implicit val seriesSummaryReference: TSType[SeriesSummaryDTO] =
      TSType.external[SeriesSummaryDTO]("ISeriesSummaryDTO")
    TSType.fromCaseClass[AudioSummaryDTO]
  }
}
