/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.frontpageapi.controller

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import no.ndla.frontpageapi.model.api.ErrorHelpers
import org.http4s.HttpRoutes
import sttp.tapir._
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
  }
}
