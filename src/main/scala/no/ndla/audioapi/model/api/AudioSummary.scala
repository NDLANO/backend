package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the audio")
case class AudioSummary(
    @(ApiModelProperty @field)(description = "The unique id of the audio") id: Long,
    @(ApiModelProperty @field)(description = "The title of the audio") title: Title,
    @(ApiModelProperty @field)(
      description = "The full url to where the complete information about the audio can be found") url: String,
    @(ApiModelProperty @field)(description = "Describes the license of the audio") license: String,
    @(ApiModelProperty @field)(description = "A list of available languages for this audio") supportedLanguages: Seq[
      String])
