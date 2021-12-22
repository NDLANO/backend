/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Status information of the resource")
case class Status(
    @(ApiModelProperty @field)(description = "The current status of the resource") current: String,
    @(ApiModelProperty @field)(description = "Previous statuses this resource has been in") other: Seq[String]
)
