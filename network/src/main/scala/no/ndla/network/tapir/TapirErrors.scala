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
  val errorOutputs = {
    oneOf[ErrorBody](
      oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFoundBody])),
      oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[GenericBody])),
      oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequestBody])),
      oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[UnauthorizedBody])),
      oneOfVariant(statusCode(StatusCode.Forbidden).and(jsonBody[ForbiddenBody])),
      oneOfVariant(statusCode(StatusCode.UnprocessableEntity).and(jsonBody[UnprocessableEntityBody])),
      oneOfVariant(statusCode(StatusCode.NotImplemented).and(jsonBody[NotImplementedBody]))
    )
  }
}
