/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.controller

import no.ndla.common.scalatra.BaseHealthController

trait HealthController {
  val healthController: HealthController

  class HealthController extends BaseHealthController {}
}
