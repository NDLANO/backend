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
  private val DURATION_INVALID  = "Value duration must be greater than 0 if present."

  def validateRequired(durationOpt: Option[Int]): Option[ValidationMessage] = {
    durationOpt match {
      case None => None
      case Some(duration) =>
        if (duration < 1) {
          Some(ValidationMessage("duration", DURATION_INVALID))
        } else {
          None
        }
    }
  }
}
