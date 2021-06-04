package no.ndla.audioapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
case class Description(
  @(ApiModelProperty @field)(description = "The description of the element") description: String,
  @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in the description") language: String
)
// format: on
