/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.validation

import no.ndla.common.errors.ValidationMessage
import no.ndla.common.model.domain.learningpath.{LearningPathStatus, StepStatus}

class StatusValidator {

  def validateLearningStepStatus(status: String): Option[ValidationMessage] = {
    if (StepStatus.valueOf(status).isEmpty) {
      Some(ValidationMessage("status", s"'$status' is not a valid status."))
    } else {
      None
    }
  }

  def validateLearningPathStatus(status: String): Option[ValidationMessage] = {
    if (LearningPathStatus.valueOf(status).isEmpty) {
      Some(ValidationMessage("status", s"'$status' is not a valid publishingstatus."))
    } else {
      None
    }
  }
}
