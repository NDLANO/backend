/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import com.scalatsi._
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

import scala.annotation.unused

// format: off
@description("Short summary of information about the audio")
case class AudioSummary(
    @description("The unique id of the audio") id: Long,
    @description("The title of the audio") title: Title,
    @description("The audioType. Possible values standard and podcast") audioType: String,
    @description("The full url to where the complete information about the audio can be found") url: String,
    @description("Describes the license of the audio") license: String,
    @description("A list of available languages for this audio") supportedLanguages: Seq[String],
    @description("A manuscript for the audio") manuscript: Option[Manuscript],
    @description("Meta information about podcast, only applicable if audioType is 'podcast'.") podcastMeta: Option[PodcastMeta],
    @description("Series that the audio is part of") series: Option[SeriesSummary],
    @description("The time and date of last update") lastUpdated: NDLADate
)
// format: on

object AudioSummary {
  implicit val AudioSummaryTSI: TSIType[AudioSummary] = {
    @unused
    implicit val seriesSummaryReference: TSType[SeriesSummary] = TSType.external[SeriesSummary]("ISeriesSummary")
    TSType.fromCaseClass[AudioSummary]
  }
}
