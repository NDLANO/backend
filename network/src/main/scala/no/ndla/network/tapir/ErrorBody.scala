/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.errors.ValidationMessage
import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try
import cats.implicits._

@description("Information about an error")
case class ErrorBody(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("When the error occured") occurredAt: LocalDateTime,
    @description("List of validation messages") messages: Option[Seq[ValidationMessage]],
    @description("Numeric http status code") statusCode: Int
)

object ErrorBody {
  implicit val msgEncoder                  = deriveEncoder[ValidationMessage]
  implicit val encoder: Encoder[ErrorBody] = deriveEncoder[ErrorBody].mapJsonObject(_.remove("statusCode"))

  def apply(
      code: String,
      description: String,
      occurredAt: LocalDateTime,
      statusCode: Int
  ): ErrorBody = {
    new ErrorBody(
      code = code,
      description = description,
      occurredAt = occurredAt,
      messages = None,
      statusCode = statusCode
    )
  }
}
