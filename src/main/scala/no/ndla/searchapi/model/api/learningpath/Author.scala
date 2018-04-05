/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about an author")
case class Author(@(ApiModelProperty@field)(description = "The description of the author. Eg. Photographer or Supplier") `type`: String,
                  @(ApiModelProperty@field)(description = "The name of the of the author") name: String)
