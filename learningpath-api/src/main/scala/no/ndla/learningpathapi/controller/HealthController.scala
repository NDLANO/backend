/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.common.scalatra.BaseHealthController

trait HealthController {
  val healthController: HealthController

  class HealthController extends BaseHealthController {}
}
