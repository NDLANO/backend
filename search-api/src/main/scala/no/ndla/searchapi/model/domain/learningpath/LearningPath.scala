/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.model.domain.{Content, Tag, Title}
import java.time.LocalDateTime

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
    lastUpdated: LocalDateTime,
    tags: List[Tag],
    owner: String,
    copyright: Copyright,
    learningsteps: List[LearningStep] = Nil
) extends Content

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, DELETED, SUBMITTED, UNLISTED = Value
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value
}
