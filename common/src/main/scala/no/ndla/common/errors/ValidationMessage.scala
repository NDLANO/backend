/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.errors

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "A message describing a validation error on a specific field")
case class ValidationMessage(
    @(ApiModelProperty @field)(description = "The field the error occured in") field: String,
    @(ApiModelProperty @field)(description = "The validation message") message: String
)

object ValidationMessage {
  def seq(fieldName: String, message: String): Seq[ValidationMessage] = {
    Seq(ValidationMessage(fieldName, message))
  }
}
