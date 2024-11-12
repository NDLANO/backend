/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.errors.{AccessDeniedException, ValidationException, ValidationMessage}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.LearningpathCopyright
import no.ndla.common.model.domain.{Tag, Title}
import no.ndla.language.Language.getSupportedLanguages
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathCombinedUser
import no.ndla.learningpathapi.validation.DurationValidator
import no.ndla.network.model.CombinedUser
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

case class LearningPath(
    id: Option[Long],
    revision: Option[Int],
    externalId: Option[String],
    isBasedOn: Option[Long],
    title: Seq[Title],
    description: Seq[Description],
    coverPhotoId: Option[String],
    duration: Option[Int],
    status: LearningPathStatus,
    verificationStatus: LearningPathVerificationStatus,
    lastUpdated: NDLADate,
    tags: Seq[Tag],
    owner: String,
    copyright: LearningpathCopyright,
    learningsteps: Option[Seq[LearningStep]] = None,
    message: Option[Message] = None
) {

  def supportedLanguages: Seq[String] = {
    val stepLanguages = learningsteps.getOrElse(Seq.empty).flatMap(_.supportedLanguages)

    (getSupportedLanguages(
      title,
      description,
      tags
    ) ++ stepLanguages).distinct
  }

  def isPrivate: Boolean = {
    status == LearningPathStatus.PRIVATE
  }

  def isPublished: Boolean = {
    status == LearningPathStatus.PUBLISHED
  }

  def isDeleted: Boolean = {
    status == LearningPathStatus.DELETED
  }

  def canSetStatus(status: LearningPathStatus, user: CombinedUser): Try[LearningPath] = {
    if (status == LearningPathStatus.PUBLISHED && !user.canPublish) {
      Failure(AccessDeniedException("You need to be a publisher to publish learningpaths."))
    } else {
      canEditLearningpath(user)
    }
  }

  def canEditLearningpath(user: CombinedUser): Try[LearningPath] = {
    if (
      user.id.contains(owner) ||
      user.isAdmin ||
      (user.isWriter && verificationStatus == LearningPathVerificationStatus.CREATED_BY_NDLA)
    ) {
      Success(this)
    } else {
      Failure(AccessDeniedException("You do not have access to the requested resource."))
    }
  }

  def isOwnerOrPublic(user: CombinedUser): Try[LearningPath] = {
    if (isPrivate) {
      canEditLearningpath(user)
    } else {
      Success(this)
    }
  }

  def canEdit(userInfo: CombinedUser): Boolean = canEditLearningpath(userInfo).isSuccess

  def lsLength: Int = learningsteps.map(_.length).getOrElse(0)

  def validateSeqNo(seqNo: Int): Unit = {
    if (seqNo < 0 || seqNo > lsLength - 1) {
      throw new ValidationException(
        errors = List(ValidationMessage("seqNo", s"seqNo must be between 0 and ${lsLength - 1}"))
      )
    }
  }

  def validateForPublishing(): Try[LearningPath] = {
    val validationResult = new DurationValidator().validateRequired(duration).toList
    if (validationResult.isEmpty)
      Success(this)
    else
      Failure(new ValidationException(errors = validationResult))
  }
}

object LearningPath extends SQLSyntaxSupport[LearningPath] {
  implicit val encoder: Encoder[LearningPath] = deriveEncoder
  implicit val decoder: Decoder[LearningPath] = deriveDecoder

  override val tableName = "learningpaths"

  def fromResultSet(lp: SyntaxProvider[LearningPath])(rs: WrappedResultSet): LearningPath =
    fromResultSet(lp.resultName)(rs)

  def fromResultSet(lp: ResultName[LearningPath])(rs: WrappedResultSet): LearningPath = {
    val jsonStr = rs.string(lp.c("document"))
    val meta    = CirceUtil.unsafeParseAs[LearningPath](jsonStr)
    meta.copy(
      id = Some(rs.long(lp.c("id"))),
      revision = Some(rs.int(lp.c("revision"))),
      externalId = rs.stringOpt(lp.c("external_id"))
    )
  }
}
