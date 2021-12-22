/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of license information")
case class License(
    @(ApiModelProperty @field)(description = "The name of the license") license: String,
    @(ApiModelProperty @field)(description = "Description of the license") description: Option[String],
    @(ApiModelProperty @field)(description = "Url to where the license can be found") url: Option[String])
