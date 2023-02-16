/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.network.scalatra.BaseHealthController

trait HealthController {
  val healthController: HealthController

  class HealthController extends BaseHealthController {}
}
