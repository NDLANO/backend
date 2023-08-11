/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
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
@description("Meta information about the audio object")
case class AudioMetaInformation(
    @description("The unique id of this audio") id: Long,
    @description("The revision number of this audio") revision: Int,
    @description("The title of the audio file") title: Title,
    @description("The audio file for this language") audioFile: Audio,
    @description("Copyright information for the audio files") copyright: Copyright,
    @description("Tags for this audio file") tags: Tag,
    @description("The languages available for this audio") supportedLanguages: Seq[String],
    @description("Type of audio. 'standard', or 'podcast'.") audioType: String,
    @description("Meta information about podcast, only applicable if audioType is 'podcast'.") podcastMeta: Option[PodcastMeta],
    @description("Meta information about series if the audio is a podcast and a part of a series.") series: Option[Series],
    @description("Manuscript for the audio") manuscript: Option[Manuscript],
    @description("The time of creation for the audio-file") created: NDLADate,
    @description("The time of last update for the audio-file") updated: NDLADate
)
// format: on

object AudioMetaInformation {
  implicit val audioMetaInformationTSI: TSIType[AudioMetaInformation] = {
    @unused
    implicit val seriesReference: TSNamedType[Series] = TSType.external[Series]("ISeries")
    TSType.fromCaseClass[AudioMetaInformation]
  }
}
