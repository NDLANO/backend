/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.model.domain.learningpath.LearningStep
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.network.model.CombinedUser

import scala.util.{Failure, Success, Try}

object ImplicitLearningStep {
  implicit class ImplicitLearningStepMethods(ls: LearningStep) {
    def canEdit(user: CombinedUser): Try[LearningStep] = {
      if (user.id.contains(ls.owner) || user.isNdla) {
        Success(ls)
      } else {
        Failure(AccessDeniedException("You do not have access to the requested resource."))
      }
    }
    def canEditLearningStep(user: CombinedUser): Boolean = canEdit(user).isSuccess
  }
}
