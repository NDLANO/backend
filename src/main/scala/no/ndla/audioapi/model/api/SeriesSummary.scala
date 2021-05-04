/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Short summary of information about the series")
case class SeriesSummary(
    @(ApiModelProperty @field)(description = "The unique id of the series") id: Long,
    @(ApiModelProperty @field)(description = "The title of the audio") title: Title,
    @(ApiModelProperty @field)(description = "A list of available languages for this series") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "A list of episode summaries") episodes: Seq[AudioSummary],
    @(ApiModelProperty @field)(description = "Cover photo for the series") coverPhoto: CoverPhoto
)
// format: on
