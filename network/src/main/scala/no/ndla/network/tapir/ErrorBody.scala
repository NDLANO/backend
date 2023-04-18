/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import java.time.LocalDateTime

sealed trait ErrorBody {
  val code: String
  val description: String
  val occurredAt: LocalDateTime
}

case class GenericBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class NotFoundBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class BadRequestBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class UnauthorizedBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class UnprocessableEntityBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class ForbiddenBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class NotImplementedBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody

case class BadGatewayBody(
    code: String,
    description: String,
    occurredAt: LocalDateTime
) extends ErrorBody
