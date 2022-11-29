/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.data.Kleisli
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import no.ndla.frontpageapi.controller.{NdlaMiddleware, Service}
import no.ndla.frontpageapi.model.api.{ErrorHelpers, GenericError}
import org.http4s.headers.`Content-Type`
import org.http4s.server.Router
import org.http4s.{Headers, HttpRoutes, MediaType, Request, Response}
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.model.ValuedEndpointOutput

import scala.annotation.unused

trait Routes {
  this: Service with NdlaMiddleware with ErrorHelpers =>

  object Routes extends StrictLogging {
    def buildBindings(routes: List[Service]): List[(String, HttpRoutes[IO])] = {
      val (docServices, noDocServices) = routes.partitionMap {
        case swaggerService: SwaggerService  => Left(swaggerService)
        case serviceWithoutDoc: NoDocService => Right(serviceWithoutDoc)
      }

      // Full paths are already prefixed in the endpoints to make nice documentation
      val swaggerBinding = "/" -> swaggerServicesToRoutes(docServices)
      noDocServices.map(_.getBinding) :+ swaggerBinding
    }

    def failureResponse(@unused _error: String): ValuedEndpointOutput[_] = {
      ValuedEndpointOutput(jsonBody[GenericError], ErrorHelpers.generic)
    }

    def swaggerServicesToRoutes(services: List[SwaggerService]): HttpRoutes[IO] = {
      val swaggerEndpoints = services.flatMap(_.builtEndpoints)
      val options          = Http4sServerOptions.customiseInterceptors[IO].defaultHandlers(failureResponse).options
      val routes           = Http4sServerInterpreter[IO](options).toRoutes(swaggerEndpoints)

      routes
    }

    def getFallbackRoute: Response[IO] = {
      val body: String = Printer.noSpaces.print(ErrorHelpers.notFound.asJson)
      val headers      = Headers(`Content-Type`(MediaType.application.json))
      Response.notFound[IO].withEntity(body).withHeaders(headers)
    }

    def build(routes: List[Service]): Kleisli[IO, Request[IO], Response[IO]] = {
      logger.info("Building swagger service")
      val bindings = buildBindings(routes)
      val router   = Router[IO](bindings: _*)
      Kleisli[IO, Request[IO], Response[IO]](req => {
        NdlaMiddleware(
          req,
          router.run(req).getOrElse {
            getFallbackRoute
          }
        )
      })
    }
  }
}
