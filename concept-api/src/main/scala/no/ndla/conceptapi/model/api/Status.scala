/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Status information for the concept")
case class Status(
    @(ApiModelProperty @field)(description = "The current status of the concept") current: String,
    @(ApiModelProperty @field)(description = "Previous statuses this concept has been in") other: Seq[String]
)
