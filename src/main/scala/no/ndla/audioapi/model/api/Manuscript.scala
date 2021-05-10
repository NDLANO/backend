package no.ndla.audioapi.model.api

import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class Manuscript(
    @(ApiModelProperty @field)(description = "The manuscript of the audio file") manuscript: String,
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in the manuscript") language: String
)
