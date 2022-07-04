/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service

import java.time.LocalDateTime

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): LocalDateTime = {
      LocalDateTime.now()
    }
  }
}
