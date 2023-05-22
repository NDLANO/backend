/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.effect.IO
import no.ndla.common.Warmup
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, Response}

trait TapirHealthController {
  this: Service =>

  class TapirHealthController extends Warmup with NoDocService {
    protected def checkHealth(): IO[Response[IO]] = Ok("Health check succeeded")

    override def getBinding: (String, HttpRoutes[IO]) = "/health" -> HttpRoutes.of[IO] { case GET -> Root =>
      if (!isWarmedUp) InternalServerError("Warmup hasn't finished")
      else checkHealth()
    }
  }
}
