/*
 * Part of NDLA common.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import java.time.LocalDateTime

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): LocalDateTime = {
      LocalDateTime.now()
    }
  }
}
