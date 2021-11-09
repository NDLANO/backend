/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Url and size information about the image")
case class NewAudioFile(@(ApiModelProperty @field)(description = "The name of the file") fileName: String,
                        @(ApiModelProperty @field)(description =
                          "ISO 639-1 code that represents the language used in the audio") language: String)
