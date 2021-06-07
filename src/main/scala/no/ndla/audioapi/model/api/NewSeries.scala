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
@ApiModel(description = "Meta information about podcast series")
case class NewSeries(
  @(ApiModelProperty @field)(description = "Header for the series") title: String,
  @(ApiModelProperty @field)(description = "Description for the series") description: String,
  @(ApiModelProperty @field)(description = "Cover photo for the series") coverPhotoId: String,
  @(ApiModelProperty @field)(description = "Cover photo alttext for the series") coverPhotoAltText: String,
  @(ApiModelProperty @field)(description = "Ids for episodes of the series") episodes: Set[Long],
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in this resource") language: String,
  @(ApiModelProperty @field)(description = "Revision number of this series (Only used to do locking when updating)") revision: Option[Int]
)
// format: on
