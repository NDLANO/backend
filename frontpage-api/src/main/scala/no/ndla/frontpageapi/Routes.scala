/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import no.ndla.frontpageapi.controller.{NdlaMiddleware, Service}
import org.http4s.HttpRoutes

trait Routes {
  this: Service with NdlaMiddleware =>

  object Routes {
    def build(routes: List[Service]): List[(String, HttpRoutes[IO])] = {
      val (docServices, noDocServices) = routes.partitionMap {
        case swaggerService: SwaggerService  => Left(swaggerService)
        case serviceWithoutDoc: NoDocService => Right(serviceWithoutDoc)
      }

      // Full paths are already prefixed in the endpoints to make nice documentation
      val swaggerBinding = "/" -> NdlaMiddleware(docServices)
      noDocServices.map(_.getBinding) :+ swaggerBinding
    }
  }
}
