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

extension (learningpath: LearningPath) {
  def canSetStatus(status: LearningPathStatus, user: CombinedUser): Try[LearningPath] = {
    if (status == LearningPathStatus.PUBLISHED && !user.canPublish) {
      Failure(AccessDeniedException("You need to be a publisher to publish learningpaths."))
    } else {
      canEditLearningpath(user)
    }
  }

  def canEditLearningpath(user: CombinedUser): Try[LearningPath] = {
    if (
      user.id.contains(learningpath.owner) ||
      user.isAdmin ||
      (user.isWriter && learningpath.verificationStatus == LearningPathVerificationStatus.CREATED_BY_NDLA)
    ) {
      Success(learningpath)
    } else {
      Failure(AccessDeniedException("You do not have access to the requested resource."))
    }
  }

  def isOwnerOrPublic(user: CombinedUser): Try[LearningPath] = {
    if (learningpath.isPrivate) {
      canEditLearningpath(user)
    } else {
      Success(learningpath)
    }
  }

  def canEditPath(userInfo: CombinedUser): Boolean = canEditLearningpath(userInfo).isSuccess

  private def lsLength: Int           = learningpath.learningsteps.map(_.length).getOrElse(0)
  def validateSeqNo(seqNo: Int): Unit = {
    if (seqNo < 0 || seqNo > lsLength - 1) {
      throw new ValidationException(
        errors = List(ValidationMessage("seqNo", s"seqNo must be between 0 and ${lsLength - 1}"))
      )
    }
  }

  def validateForPublishing(): Try[LearningPath] = {
    val validationResult = new DurationValidator().validateRequired(learningpath.duration).toList
    if (validationResult.isEmpty)
      Success(learningpath)
    else
      Failure(new ValidationException(errors = validationResult))
  }

}
