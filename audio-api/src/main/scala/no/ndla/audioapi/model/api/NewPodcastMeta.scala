/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information about podcast audio")
case class NewPodcastMeta(
  @(ApiModelProperty @field)(description = "Introduction for the podcast") introduction: String,
  @(ApiModelProperty @field)(description = "Cover photo for the podcast") coverPhotoId: String,
  @(ApiModelProperty @field)(description = "Cover photo alttext for the podcast") coverPhotoAltText: String,
)
// format: on
