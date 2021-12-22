/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of the tags of the audio")
case class Tag(
    @(ApiModelProperty @field)(description = "The searchable tag.") tags: Seq[String],
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in tag") language: String)
