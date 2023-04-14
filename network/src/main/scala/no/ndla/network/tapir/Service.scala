/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.network.tapir

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.configuration.HasBaseProps
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait Service {
  this: NdlaMiddleware with HasBaseProps =>

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
          endpoint = e.endpoint.prependIn(this.prefix).tag(props.ApplicationName),
          securityLogic = e.securityLogic,
          logic = e.logic
        )
      })
    }
  }
}
