package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "The license for the audio") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the audio is procured") origin: Option[String],
                     @(ApiModelProperty@field)(description = "") authors: Seq[Author])
