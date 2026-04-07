/*
 * Part of NDLA common
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import no.ndla.common.model.NDLADate

class Clock {
  def now(): NDLADate = {
    Clock.now()
  }
}
object Clock {
  def apply(): Clock  = new Clock()
  def now(): NDLADate = NDLADate.now()
}
