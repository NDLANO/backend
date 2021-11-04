/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import com.scalatsi._
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Short summary of information about the series")
case class SeriesSummary(
    @(ApiModelProperty @field)(description = "The unique id of the series") id: Long,
    @(ApiModelProperty @field)(description = "The title of the series") title: Title,
    @(ApiModelProperty @field)(description = "The description of the series") description: Description,
    @(ApiModelProperty @field)(description = "A list of available languages for this series") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "A list of episode summaries") episodes: Option[Seq[AudioSummary]],
    @(ApiModelProperty @field)(description = "Cover photo for the series") coverPhoto: CoverPhoto
)
// format: on

object SeriesSummary {
  implicit val SeriesSummaryTSI: TSIType[SeriesSummary] = {
    implicit val audioSummaryReference: TSType[AudioSummary] = TSType.external[AudioSummary]("IAudioSummary")
    TSType.fromCaseClass[SeriesSummary]
  }
}
