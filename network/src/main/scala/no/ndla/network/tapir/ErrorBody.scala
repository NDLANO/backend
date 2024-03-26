/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.EncoderOps
import no.ndla.common.DateParser
import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

sealed trait AllErrors {
  val statusCode: Int
}

object AllErrors {
  def removeStatusCode[A](encoder: Encoder.AsObject[A]): Encoder.AsObject[A] = {
    encoder.mapJsonObject(_.remove("statusCode"))
  }

  implicit val dateTimeEncoder: Encoder[LocalDateTime] = DateParser.Circe.localDateTimeEncoder
  implicit val dateTimeDecoder: Decoder[LocalDateTime] = DateParser.Circe.localDateTimeDecoder

  implicit val notFound: Encoder[NotFoundWithSupportedLanguages] = removeStatusCode(
    deriveEncoder[NotFoundWithSupportedLanguages]
  )
  implicit val msgEncoder: Encoder[ValidationMessage]            = deriveEncoder[ValidationMessage]
  implicit val validationErrorBody: Encoder[ValidationErrorBody] = removeStatusCode(deriveEncoder[ValidationErrorBody])
  implicit val errorBody: Encoder[ErrorBody]                     = removeStatusCode(deriveEncoder[ErrorBody])

  implicit val encoder: Encoder[AllErrors] = Encoder.instance {
    case s: ErrorBody                       => s.asJson
    case vb: ValidationErrorBody            => vb.asJson
    case nf: NotFoundWithSupportedLanguages => nf.asJson
  }
}

@description("Information about an error")
case class ErrorBody(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("When the error occurred") occurredAt: NDLADate,
    @description("Numeric http status code") override val statusCode: Int
) extends AllErrors

@description("Information about an error")
case class ValidationErrorBody(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("When the error occurred") occurredAt: NDLADate,
    @description("List of validation messages") messages: Option[Seq[ValidationMessage]],
    @description("Numeric http status code") override val statusCode: Int
) extends AllErrors

@description("Information about an error")
case class NotFoundWithSupportedLanguages(
    @description("Code stating the type of error") code: String,
    @description("Description of the error") description: String,
    @description("When the error occurred") occurredAt: NDLADate,
    @description("List of supported languages") supportedLanguages: Seq[String],
    @description("Numeric http status code") override val statusCode: Int
) extends AllErrors
