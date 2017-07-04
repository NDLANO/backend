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

import scala.annotation.meta.field


@ApiModel(description = "Meta information about the audio object")
case class AudioMetaInformation(@(ApiModelProperty@field)(description = "The unique id of this audio") id: Long,
                                @(ApiModelProperty@field)(description = "The titles of the audio file") titles: String,
                                @(ApiModelProperty@field)(description = "The audio files in different languages") audioFiles: Seq[Audio],
                                @(ApiModelProperty@field)(description = "Copyright information for the audio files") copyright: Copyright,
                                @(ApiModelProperty@field)(description = "Tags for this audio file") tags: Seq[Tag])
