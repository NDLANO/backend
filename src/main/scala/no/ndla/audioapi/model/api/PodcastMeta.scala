package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Meta information about podcast audio")
case class PodcastMeta(
  @(ApiModelProperty @field)(description = "Header for the podcast") header: String,
  @(ApiModelProperty @field)(description = "Introduction for the podcast") introduction: String,
  @(ApiModelProperty @field)(description = "Cover photo for the podcast") coverPhoto: CoverPhoto,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in the title") language: String
)
// format: on
