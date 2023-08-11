/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.network.tapir

import cats.effect.IO
import io.circe.{Decoder, Encoder}
import no.ndla.common.DateParser
import no.ndla.common.configuration.HasBaseProps
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

import java.time.LocalDateTime

trait Service {
  this: NdlaMiddleware with HasBaseProps =>

  import NoNullJsonPrinter._

  sealed trait Service {

    /** Helper to simplify returning _both_ NoContent and some json body T from an endpoint */
    def noContentOrBodyOutput[T: Encoder: Decoder: Schema] = oneOf[Option[T]](
      oneOfVariantValueMatcher(StatusCode.Ok, jsonBody[Option[T]]) { case Some(_) => true },
      oneOfVariantValueMatcher(StatusCode.NoContent, jsonBody[Option[T]]) { case None => true }
    )

    // Since we don't "own" the `LocalDateTime` class we cannot automatically include encoder/decoders
    // in the companion object.
    protected implicit val dateTimeEncoder: Encoder[LocalDateTime] = DateParser.Circe.localDateTimeEncoder
    protected implicit val dateTimeDecoder: Decoder[LocalDateTime] = DateParser.Circe.localDateTimeDecoder
  }

  trait NoDocService extends Service {
    def getBinding: (String, HttpRoutes[IO])
  }

  trait SwaggerService extends Service {
    val enableSwagger: Boolean = true
    val serviceName: String    = this.getClass.getSimpleName
    protected val prefix: EndpointInput[Unit]
    protected val endpoints: List[ServerEndpoint[Any, IO]]

    lazy val builtEndpoints: List[ServerEndpoint[Any, IO]] = {
      this.endpoints.map(e => {
        ServerEndpoint(
          endpoint = e.endpoint.prependIn(this.prefix).tag(this.serviceName),
          securityLogic = e.securityLogic,
          logic = e.logic
        )
      })
    }
  }
}
