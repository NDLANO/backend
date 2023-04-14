/*
 * Part of NDLA network.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

object TapirErrors {
  val NotFoundVariant: EndpointOutput[ErrorBody] =
    statusCode(StatusCode.NotFound).and(jsonBody[ErrorBody])

  val GenericVariant: EndpointOutput[ErrorBody] =
    statusCode(StatusCode.InternalServerError).and(jsonBody[ErrorBody])

  val BadRequestVariant: EndpointOutput[ErrorBody] =
    statusCode(StatusCode.BadRequest).and(jsonBody[ErrorBody])

  val UnauthorizedVariant: EndpointOutput[ErrorBody] =
    statusCode(StatusCode.Unauthorized).and(jsonBody[ErrorBody])

  val ForbiddenVariant: EndpointOutput[ErrorBody] =
    statusCode(StatusCode.Forbidden).and(jsonBody[ErrorBody])

  val UnprocessableEntityVariant: EndpointOutput[ErrorBody] =
    statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ErrorBody])

  val errorOutputs: EndpointOutput.OneOf[ErrorBody, ErrorBody] =
    oneOf[ErrorBody](
      oneOfVariant(NotFoundVariant),
      oneOfVariant(GenericVariant),
      oneOfVariant(BadRequestVariant),
      oneOfVariant(UnauthorizedVariant),
      oneOfVariant(ForbiddenVariant),
      oneOfVariant(UnprocessableEntityVariant)
    )
}
