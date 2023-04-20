/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.oembedproxy.controller

import cats.effect.IO
import no.ndla.common.Warmup
import no.ndla.network.tapir.Service
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

trait HealthController {
  this: Service =>

  val healthController: HealthController

  class HealthController extends Warmup with NoDocService {
    override def getBinding: (String, HttpRoutes[IO]) = "/health" -> {
      HttpRoutes.of[IO] { case GET -> Root =>
        if (isWarmedUp) Ok("Health check succeeded")
        else InternalServerError("Warmup hasn't finished")
      }
    }
  }
}
