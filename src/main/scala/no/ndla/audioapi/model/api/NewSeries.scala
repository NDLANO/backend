package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information about podcast series")
case class NewSeries(
  @(ApiModelProperty @field)(description = "Header for the podcast") title: String,
  @(ApiModelProperty @field)(description = "Cover photo for the podcast") coverPhotoId: String,
  @(ApiModelProperty @field)(description = "Cover photo alttext for the podcast") coverPhotoAltText: String,
  @(ApiModelProperty @field)(description = "Ids for episodes of the podcast") episodes: Set[Long],
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in this resource") language: String,
  @(ApiModelProperty @field)(description = "Revision number of this series (Only used to do locking when updating)") revision: Option[Int]
)
// format: on
