/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.IO
import io.circe.generic.auto._
import no.ndla.common.CorrelationID
import no.ndla.common.RequestLogger.{afterRequestLogString, beforeRequestLogString}
import no.ndla.frontpageapi.model.api.{ErrorHelpers, GenericError}
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.NdlaHttpRequest
import org.http4s.{HttpRoutes, Request, Response}
import org.log4s.getLogger
import org.typelevel.ci.CIString
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.model.ValuedEndpointOutput

import scala.annotation.unused

trait NdlaMiddleware {
  this: ErrorHelpers with Service =>
  object NdlaMiddleware {
    private val CorrelationIdHeader = CIString("X-Correlation-ID")
    private val logger              = getLogger

    def asNdlaHttpRequest[F[+_]](req: Request[F]): NdlaHttpRequest =
      NdlaHttpRequest(
        serverPort = req.serverPort.map(_.value).getOrElse(-1),
        getHeaderFunc = name => req.headers.get(CIString(name)).map(_.head.value),
        getScheme = req.uri.scheme.map(_.value).getOrElse("http"),
        serverName = req.serverAddr.map(_.toUriString).getOrElse("localhost"),
        servletPath = req.uri.path.renderString
      )

    private def before(service: HttpRoutes[IO]): HttpRoutes[IO] = cats.data.Kleisli { req: Request[IO] =>
      val beforeTime = System.currentTimeMillis()

      val correlationIdHeader = req.headers.get(CorrelationIdHeader).map(_.head.value)
      CorrelationID.set(correlationIdHeader)
      ApplicationUrl.set(asNdlaHttpRequest(req))
      logger.info(
        beforeRequestLogString(
          method = req.method.name,
          requestPath = req.uri.path.renderString,
          queryString = req.queryString
        )
      )

      service(req).map { resp =>
        after(beforeTime, req, resp)
      }
    }

    private def after(beforeTime: Long, req: Request[IO], resp: Response[IO]): Response[IO] = {
      val latency = System.currentTimeMillis() - beforeTime

      logger.info(
        afterRequestLogString(
          method = req.method.name,
          requestPath = req.uri.path.renderString,
          queryString = req.queryString,
          latency = latency,
          responseCode = resp.status.code
        )
      )

      CorrelationID.clear()
      ApplicationUrl.clear()

      resp
    }

    def failureResponse(@unused _error: String): ValuedEndpointOutput[_] = {
      ValuedEndpointOutput(jsonBody[GenericError], ErrorHelpers.generic)
    }

    def apply(services: List[SwaggerService]): HttpRoutes[IO] = {
      val swaggerEndpoints = services.flatMap(_.builtEndpoints)
      val options          = Http4sServerOptions.customiseInterceptors[IO].defaultHandlers(failureResponse).options
      val routes           = Http4sServerInterpreter[IO](options).toRoutes(swaggerEndpoints)

      before(routes)
    }

    def apply(service: HttpRoutes[IO]): HttpRoutes[IO] = before(service)
  }
}
