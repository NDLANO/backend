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

@ApiModel(description = "Description of license information")
case class License(
    @(ApiModelProperty @field)(description = "The name of the license") license: String,
    @(ApiModelProperty @field)(description = "Description of the license") description: Option[String],
    @(ApiModelProperty @field)(description = "Url to where the license can be found") url: Option[String])
