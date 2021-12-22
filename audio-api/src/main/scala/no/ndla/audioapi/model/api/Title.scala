/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class Title(@(ApiModelProperty @field)(description = "The title of the audio file") title: String,
                 @(ApiModelProperty @field)(
                   description = "ISO 639-1 code that represents the language used in the title") language: String)
