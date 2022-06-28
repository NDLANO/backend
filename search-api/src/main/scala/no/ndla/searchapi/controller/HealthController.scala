/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.search.Elastic4sClient
import org.scalatra.{Ok, ScalatraServlet}

trait HealthController {
  this: Elastic4sClient =>
  val healthController: HealthController

  class HealthController extends ScalatraServlet with LazyLogging {
    get("/") {
      Ok()
    }
  }
}
