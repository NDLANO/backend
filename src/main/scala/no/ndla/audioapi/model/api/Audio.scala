package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Url and size information about the image")
case class Audio(@(ApiModelProperty@field)(description = "The path to where the audio is located") url: String,
                 @(ApiModelProperty@field)(description = "The mime type of the audio file") mimeType: String,
                 @(ApiModelProperty@field)(description = "The size of the audio file") fileSize: Long)
