/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._

object HealthController {

  def apply(): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root => Ok("Health check succeeded")
  }
}
