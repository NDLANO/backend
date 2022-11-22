/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.scalatra.BaseHealthController
import no.ndla.imageapi.Props
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.network.ApplicationUrl

trait HealthController {
  this: ImageRepository with Props =>
  val healthController: HealthController

  class HealthController extends BaseHealthController {

    before() {
      ApplicationUrl.set(request)
    }

    after() {
      ApplicationUrl.clear()
    }
  }

}
