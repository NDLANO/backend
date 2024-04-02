/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import no.ndla.common.model.NDLADate

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): NDLADate = {
      NDLADate.now()
    }
  }
}
