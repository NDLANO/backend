/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import java.util.Date

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): Date = {
      new Date()
    }
  }
}
