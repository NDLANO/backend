/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.frontpageapi.controller

import no.ndla.frontpageapi.model.api.ErrorHelpers
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import io.circe.generic.auto._
import no.ndla.frontpageapi.model.api._
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

trait Service {
  this: ErrorHelpers with NdlaMiddleware =>

  sealed trait Service {}

  trait NoDocService extends Service {
    def getBinding: (String, HttpRoutes[IO])
  }

  trait SwaggerService extends Service with StrictLogging {
    val enableSwagger: Boolean = true
    protected val prefix: EndpointInput[Unit]
    protected val endpoints: List[ServerEndpoint[Any, IO]]

    lazy val builtEndpoints: List[ServerEndpoint[Any, IO]] = {
      this.endpoints.map(e => {
        ServerEndpoint(
          endpoint = e.endpoint.prependIn(this.prefix).tag("frontpage-api"),
          securityLogic = e.securityLogic,
          logic = e.logic
        )
      })
    }

    protected val NotFoundError: EndpointOutput[NotFoundError] =
      statusCode(StatusCode.NotFound).and(jsonBody[NotFoundError])
    protected val GenericError: EndpointOutput[GenericError] =
      statusCode(StatusCode.InternalServerError).and(jsonBody[GenericError])
    protected val BadRequestError: EndpointOutput[BadRequestError] =
      statusCode(StatusCode.BadRequest).and(jsonBody[BadRequestError])
    protected val UnprocessableEntityError: EndpointOutput[UnprocessableEntityError] =
      statusCode(StatusCode.UnprocessableEntity).and(jsonBody[UnprocessableEntityError])
    protected val ForbiddenError: EndpointOutput[ForbiddenError] =
      statusCode(StatusCode.Forbidden).and(jsonBody[ForbiddenError])
    protected val UnauthorizedError: EndpointOutput[UnauthorizedError] =
      statusCode(StatusCode.Unauthorized).and(jsonBody[UnauthorizedError])
  }
}
