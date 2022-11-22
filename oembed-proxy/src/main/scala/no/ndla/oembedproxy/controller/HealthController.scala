/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.common.scalatra.BaseHealthController

trait HealthController {
  val healthController: HealthController

  class HealthController extends BaseHealthController {}
}
