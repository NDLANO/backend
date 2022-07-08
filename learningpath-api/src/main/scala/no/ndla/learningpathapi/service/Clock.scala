/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import java.time.LocalDateTime

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): LocalDateTime = {
      LocalDateTime.now()
    }
  }
}
