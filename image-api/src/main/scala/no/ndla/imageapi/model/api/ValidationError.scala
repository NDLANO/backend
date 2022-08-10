/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import no.ndla.common.errors.ValidationMessage
import java.time.LocalDateTime
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about validation errors")
case class ValidationError(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String,
    @(ApiModelProperty @field)(description = "Description of the error") description: String = "Validation error",
    @(ApiModelProperty @field)(description = "List of validation messages") messages: Seq[ValidationMessage],
    @(ApiModelProperty @field)(description = "When the error occurred") occurredAt: LocalDateTime = LocalDateTime.now()
)
