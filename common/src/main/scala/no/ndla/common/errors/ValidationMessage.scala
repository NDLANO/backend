/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.errors

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("A message describing a validation error on a specific field")
case class ValidationMessage(
    @description("The field the error occured in")
    field: String,
    @description("The validation message")
    message: String,
)

object ValidationMessage {
  implicit val encoder: Encoder[ValidationMessage] = deriveEncoder
  implicit val decoder: Decoder[ValidationMessage] = deriveDecoder

  def seq(fieldName: String, message: String): Seq[ValidationMessage] = {
    Seq(ValidationMessage(fieldName, message))
  }
}
