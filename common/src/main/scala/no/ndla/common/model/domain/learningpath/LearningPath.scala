/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{Comment, Content, Responsible, Tag, Title}
import no.ndla.language.Language.getSupportedLanguages

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
    created: NDLADate,
    lastUpdated: NDLADate,
    tags: Seq[Tag],
    owner: String,
    copyright: LearningpathCopyright,
    isMyNDLAOwner: Boolean,
    learningsteps: Option[Seq[LearningStep]] = None,
    message: Option[Message] = None,
    madeAvailable: Option[NDLADate] = None,
    responsible: Option[Responsible],
    comments: Seq[Comment]
) extends Content {

  def supportedLanguages: Seq[String] = {
    val stepLanguages = learningsteps.getOrElse(Seq.empty).flatMap(_.supportedLanguages)

    (getSupportedLanguages(
      title,
      description,
      tags
    ) ++ stepLanguages).distinct
  }

  def isPrivate: Boolean   = Seq(LearningPathStatus.PRIVATE, LearningPathStatus.READY_FOR_SHARING).contains(status)
  def isPublished: Boolean = status == LearningPathStatus.PUBLISHED
  def isDeleted: Boolean   = status == LearningPathStatus.DELETED

}

object LearningPath {
  // NOTE: We remove learningsteps from the JSON object before decoding it since it is stored in a separate table
  implicit val encoder: Encoder[LearningPath] = deriveEncoder[LearningPath].mapJsonObject(_.remove("learningsteps"))
  implicit val decoder: Decoder[LearningPath] = deriveDecoder[LearningPath].prepare { obj =>
    val learningsteps = obj.downField("learningsteps")
    if (learningsteps.succeeded) learningsteps.delete
    else obj
  }
}
