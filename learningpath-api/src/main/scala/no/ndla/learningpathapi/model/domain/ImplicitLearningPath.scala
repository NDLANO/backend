/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.errors.{AccessDeniedException, ValidationException, ValidationMessage}
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningPathVerificationStatus}
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.validation.DurationValidator
import no.ndla.network.model.CombinedUser

import scala.util.{Failure, Success, Try}

extension (learningPath: LearningPath) {
  def canSetStatus(status: LearningPathStatus, user: CombinedUser): Try[LearningPath] = {
    if (status == LearningPathStatus.PUBLISHED && !user.canPublish) {
      Failure(AccessDeniedException("You need to be a publisher to publish learningpaths."))
    } else {
      canEditLearningPath(user)
    }
  }

  def canEditLearningPath(user: CombinedUser): Try[LearningPath] = {
    if (
      user.id.contains(learningPath.owner) ||
      user.isAdmin ||
      (user.isWriter && learningPath.verificationStatus == LearningPathVerificationStatus.CREATED_BY_NDLA)
    ) {
      Success(learningPath)
    } else {
      Failure(AccessDeniedException("You do not have access to the requested resource."))
    }
  }

  def isOwnerOrPublic(user: CombinedUser): Try[LearningPath] = {
    if (learningPath.isPrivate) {
      canEditLearningPath(user)
    } else {
      Success(learningPath)
    }
  }

  def canEditPath(userInfo: CombinedUser): Boolean = canEditLearningPath(userInfo).isSuccess

  def validateSeqNo(seqNo: Int): Unit = {
    if (seqNo < 0 || seqNo > learningPath.learningsteps.length - 1) {
      throw new ValidationException(errors =
        List(ValidationMessage("seqNo", s"seqNo must be between 0 and ${learningPath.learningsteps.length - 1}"))
      )
    }
  }

  def validateForPublishing(): Try[LearningPath] = {
    val validationResult = new DurationValidator().validateRequired(learningPath.duration).toList
    if (validationResult.isEmpty) Success(learningPath)
    else Failure(new ValidationException(errors = validationResult))
  }

}
