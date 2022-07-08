/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import java.time.LocalDateTime

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): LocalDateTime = {
      LocalDateTime.now()
    }
  }
}
