/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{Content, Tag, Title}
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
    learningsteps: Option[Seq[LearningStep]] = None,
    message: Option[Message] = None,
    madeAvailable: Option[NDLADate] = None
) extends Content {

  def supportedLanguages: Seq[String] = {
    val stepLanguages = learningsteps.getOrElse(Seq.empty).flatMap(_.supportedLanguages)

    (getSupportedLanguages(
      title,
      description,
      tags
    ) ++ stepLanguages).distinct
  }

  def isPrivate: Boolean   = status == LearningPathStatus.PRIVATE
  def isPublished: Boolean = status == LearningPathStatus.PUBLISHED
  def isDeleted: Boolean   = status == LearningPathStatus.DELETED

}

object LearningPath {
  implicit val encoder: Encoder[LearningPath] = deriveEncoder
  implicit val decoder: Decoder[LearningPath] = deriveDecoder

}
