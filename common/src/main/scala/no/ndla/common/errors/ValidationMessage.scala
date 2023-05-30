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
import sttp.tapir.Schema.annotations.description

@description("A message describing a validation error on a specific field")
@ApiModel(description = "A message describing a validation error on a specific field")
case class ValidationMessage(
    @description("The field the error occured in")
    @ApiModelProperty(description = "The field the error occured in")
    field: String,
    @description("The validation message")
    @ApiModelProperty(description = "The validation message")
    message: String
)

object ValidationMessage {
  def seq(fieldName: String, message: String): Seq[ValidationMessage] = {
    Seq(ValidationMessage(fieldName, message))
  }
}
