/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import java.util.Date
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information about the audio object")
case class AudioMetaInformation(
    @(ApiModelProperty @field)(description = "The unique id of this audio") id: Long,
    @(ApiModelProperty @field)(description = "The revision number of this audio") revision: Int,
    @(ApiModelProperty @field)(description = "The title of the audio file") title: Title,
    @(ApiModelProperty @field)(description = "The audio file for this language") audioFile: Audio,
    @(ApiModelProperty @field)(description = "Copyright information for the audio files") copyright: Copyright,
    @(ApiModelProperty @field)(description = "Tags for this audio file") tags: Tag,
    @(ApiModelProperty @field)(description = "The languages available for this audio") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "Type of audio. 'standard', or 'podcast'.") audioType: String,
    @(ApiModelProperty @field)(description = "Meta information about podcast, only applicable if audioType is 'podcast'.") podcastMeta: Option[PodcastMeta],
    @(ApiModelProperty @field)(description = "Meta information about series if the audio is a podcast and a part of a series.") series: Option[Series],
    @(ApiModelProperty @field)(description = "Meta information about series if the audio is a podcast and a part of a series.") seriesId: Option[Long],
    @(ApiModelProperty @field)(description = "Manuscript for the audio") manuscript: Option[Manuscript],
    @(ApiModelProperty @field)(description = "The time of creation for the audio-file") created: Date,
    @(ApiModelProperty @field)(description = "The time of last update for the audio-file") updated: Date
)
// format: on
