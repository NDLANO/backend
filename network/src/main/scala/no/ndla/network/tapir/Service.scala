/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.network.tapir

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.common.configuration.HasBaseProps
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait Service {
  this: NdlaMiddleware with HasBaseProps =>

  import NoNullJsonPrinter._

  sealed trait Service {

    /** Helper to simplify returning _both_ NoContent and some json body T from an endpoint */
    def noContentOrBodyOutput[T: Encoder: Decoder: Schema] = oneOf[Option[T]](
      oneOfVariantValueMatcher(StatusCode.Ok, jsonBody[Option[T]]) { case Some(_) => true },
      oneOfVariantValueMatcher(StatusCode.NoContent, jsonBody[Option[T]]) { case None => true }
    )
  }

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
