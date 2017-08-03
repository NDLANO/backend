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
case class NewAudioMetaInformation(@(ApiModelProperty@field)(description = "The title of the audio file") title: String,
                                   @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in this resource") language: String,
                                   @(ApiModelProperty@field)(description = "Copyright information for the audio files") copyright: Copyright,
                                   @(ApiModelProperty@field)(description = "Tags for this audio file") tags: Seq[String])
