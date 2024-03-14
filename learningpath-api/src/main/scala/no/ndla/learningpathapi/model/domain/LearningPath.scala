/*
 * Part of NDLA learningpath-api.
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
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathTokenUser
import no.ndla.learningpathapi.validation.DurationValidator
import no.ndla.network.tapir.auth.TokenUser
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
    status: LearningPathStatus.Value,
    verificationStatus: LearningPathVerificationStatus.Value,
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

  def canSetStatus(status: LearningPathStatus.Value, user: TokenUser): Try[LearningPath] = {
    if (status == LearningPathStatus.PUBLISHED && !user.canPublish) {
      Failure(AccessDeniedException("You need to be a publisher to publish learningpaths."))
    } else {
      canEditLearningpath(user)
    }
  }

  def canEditLearningpath(user: TokenUser): Try[LearningPath] = {
    if (
      (user.id == owner) ||
      user.isAdmin ||
      (user.isWriter && verificationStatus == LearningPathVerificationStatus.CREATED_BY_NDLA)
    ) {
      Success(this)
    } else {
      Failure(AccessDeniedException("You do not have access to the requested resource."))
    }
  }

  def isOwnerOrPublic(user: TokenUser): Try[LearningPath] = {
    if (isPrivate) {
      canEditLearningpath(user)
    } else {
      Success(this)
    }
  }

  def canEdit(userInfo: TokenUser): Boolean = canEditLearningpath(userInfo).isSuccess

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

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, DELETED, UNLISTED, SUBMITTED = Value

  def valueOf(s: String): Option[LearningPathStatus.Value] = {
    LearningPathStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrError(status: String): LearningPathStatus.Value = {
    valueOf(status) match {
      case Some(status) => status
      case None =>
        throw new ValidationException(
          errors = List(ValidationMessage("status", s"'$status' is not a valid publishingstatus."))
        )
    }
  }

  def valueOfOrDefault(s: String): LearningPathStatus.Value = {
    valueOf(s).getOrElse(LearningPathStatus.PRIVATE)
  }

  implicit val encoder: Encoder[LearningPathStatus.Value] = Encoder.encodeEnumeration(LearningPathStatus)
  implicit val decoder: Decoder[LearningPathStatus.Value] = Decoder.decodeEnumeration(LearningPathStatus)
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value

  def valueOf(s: String): Option[LearningPathVerificationStatus.Value] = {
    LearningPathVerificationStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrDefault(s: String): LearningPathVerificationStatus.Value = {
    valueOf(s).getOrElse(LearningPathVerificationStatus.EXTERNAL)
  }
  implicit val encoder: Encoder[LearningPathVerificationStatus.Value] =
    Encoder.encodeEnumeration(LearningPathVerificationStatus)
  implicit val decoder: Decoder[LearningPathVerificationStatus.Value] =
    Decoder.decodeEnumeration(LearningPathVerificationStatus)
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
