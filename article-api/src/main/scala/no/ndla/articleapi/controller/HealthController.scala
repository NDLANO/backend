/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.common.scalatra.BaseHealthController

trait HealthController {
  val healthController: HealthController

  class HealthController extends BaseHealthController {}
}
