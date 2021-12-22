/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.service

import java.util.Date

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): Date = {
      new Date()
    }
  }
}
