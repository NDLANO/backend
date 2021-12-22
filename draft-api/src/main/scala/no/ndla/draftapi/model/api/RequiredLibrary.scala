/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Information about a library required to render the article")
case class RequiredLibrary(
    @(ApiModelProperty @field)(description = "The type of the library. E.g. CSS or JavaScript") mediaType: String,
    @(ApiModelProperty @field)(description = "The name of the library") name: String,
    @(ApiModelProperty @field)(description = "The full url to where the library can be downloaded") url: String)
