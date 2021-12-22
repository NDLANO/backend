/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import com.scalatsi._
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import java.util.Date
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Short summary of information about the audio")
case class AudioSummary(
    @(ApiModelProperty @field)(description = "The unique id of the audio") id: Long,
    @(ApiModelProperty @field)(description = "The title of the audio") title: Title,
    @(ApiModelProperty @field)(description = "The audioType. Possible values standard and podcast") audioType: String,
    @(ApiModelProperty @field)(description = "The full url to where the complete information about the audio can be found") url: String,
    @(ApiModelProperty @field)(description = "Describes the license of the audio") license: String,
    @(ApiModelProperty @field)(description = "A list of available languages for this audio") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "A manuscript for the audio") manuscript: Option[Manuscript],
    @(ApiModelProperty @field)(description = "Meta information about podcast, only applicable if audioType is 'podcast'.") podcastMeta: Option[PodcastMeta],
    @(ApiModelProperty @field)(description = "Series that the audio is part of") series: Option[SeriesSummary],
    @(ApiModelProperty @field)(description = "The time and date of last update") lastUpdated: Date
)
// format: on
object AudioSummary {
  implicit val AudioSummaryTSI: TSIType[AudioSummary] = {
    implicit val seriesSummaryReference: TSType[SeriesSummary] = TSType.external[SeriesSummary]("ISeriesSummary")
    TSType.fromCaseClass[AudioSummary]
  }
}
