/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.network.ApplicationUrl
import org.scalatra._

trait HealthController {
  this: ImageRepository with Props =>
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    before() {
      ApplicationUrl.set(request)
    }

    after() {
      ApplicationUrl.clear()
    }

    get("/") {
      Ok()
    }

  }

}
