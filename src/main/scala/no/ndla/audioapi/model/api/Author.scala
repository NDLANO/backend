package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModelProperty
import scala.annotation.meta.field

case class Author(
    @(ApiModelProperty @field)(description = "The description of the author. Eg. author or publisher") `type`: String,
    @(ApiModelProperty @field)(description = "The name of the of the author") name: String)
