/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import java.time.LocalDateTime

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about validation errors")
case class ValidationError(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String,
    @(ApiModelProperty @field)(description = "List of validation messages") messages: Seq[ValidationMessage],
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: LocalDateTime = LocalDateTime.now()
)
