/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.ValidationMessage

class DurationValidator {
  private val DURATION_INVALID  = "Required value duration must be greater than 0."
  private val DURATION_REQUIRED = "Required value is empty."

  def validateRequired(durationOpt: Option[Int]): Option[ValidationMessage] = {
    durationOpt match {
      case None => Some(ValidationMessage("duration", DURATION_REQUIRED))
      case Some(duration) =>
        duration < 1 match {
          case true  => Some(ValidationMessage("duration", DURATION_INVALID))
          case false => None
        }
    }
  }
}
