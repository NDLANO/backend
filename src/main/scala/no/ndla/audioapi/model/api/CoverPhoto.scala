package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information about podcast audio")
case class CoverPhoto(
    @(ApiModelProperty @field)(description = "Id for the coverPhoto in image-api") id: String,
    @(ApiModelProperty @field)(description = "Url to the coverPhoto") url: String,
    @(ApiModelProperty @field)(description = "Alttext for the coverPhoto") altText: String,
)
