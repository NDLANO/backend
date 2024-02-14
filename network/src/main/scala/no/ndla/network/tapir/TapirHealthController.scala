/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import no.ndla.common.Warmup
import sttp.model.StatusCode
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

trait TapirHealthController {
  class TapirHealthController[F[_]] extends Warmup with Service[F] {
    override val enableSwagger: Boolean = false
    val prefix: EndpointInput[Unit]     = "health"

    protected def checkHealth(): Either[String, String] = Right("Health check succeeded")

    override val endpoints: List[ServerEndpoint[Any, F]] = List(
      endpoint.get
        .out(stringBody)
        .errorOut(
          statusCode(StatusCode.InternalServerError)
            .and(stringBody)
        )
        .serverLogicPure { _ =>
          if (!isWarmedUp) Left("Warmup hasn't finished")
          else checkHealth()
        }
    )
  }
}
