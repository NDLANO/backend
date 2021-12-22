/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Search result for audio api")
case class AudioResult(
    @(ApiModelProperty @field)(description = "The unique id of this audio") id: Long,
    @(ApiModelProperty @field)(description = "The title of this audio") title: Title,
    @(ApiModelProperty @field)(description = "A direct link to the audio") url: String,
    @(ApiModelProperty @field)(description = "List of supported languages") supportedLanguages: Seq[String])
