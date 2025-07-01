/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.common.Warmup
import sttp.model.StatusCode
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*

trait TapirHealthController {
  this: TapirController =>
  val healthController: TapirHealthController
  class TapirHealthController extends Warmup with TapirController {
    @volatile private var isShuttingDown: Boolean = false
    override val enableSwagger: Boolean           = false
    val prefix: EndpointInput[Unit]               = "health"

    def setShuttingDown(): Unit = { isShuttingDown = true }

    private def checkLiveness(): Either[String, String] = Right("Healthy")
    protected def checkReadiness(): Either[String, String] = {
      if (isShuttingDown) Left("Service is shutting down")
      else if (isWarmedUp) Right("Ready")
      else Left("Service is not ready")
    }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      endpoint.get
        .description("Readiness probe. Returns 200 if the service is ready to serve traffic.")
        .in("readiness")
        .out(stringBody)
        .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
        .serverLogicPure(_ => checkReadiness()),
      endpoint.get
        .description("Liveness probe. Returns 200 if the service is alive, but not necessarily ready.")
        .in("liveness")
        .out(stringBody)
        .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
        .serverLogicPure(_ => checkLiveness()),
      endpoint.get
        .out(stringBody)
        .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
        .serverLogicPure(_ => checkLiveness())
    )
  }
}
