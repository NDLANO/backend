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
      // Only allow editing of copied learning steps where article id or embedUrl is set
      if (user.id.contains(ls.owner) || (ls.articleId.isDefined && ls.embedUrl.isEmpty) || user.isNdla) {
        Success(ls)
      } else {
        Failure(AccessDeniedException("You do not have access to the requested resource."))
      }
    }
    def canEditLearningStep(user: CombinedUser): Boolean = canEdit(user).isSuccess
  }
}
