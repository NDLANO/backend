/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.LearningpathCopyright
import no.ndla.common.model.domain.{Content, Tag, Title}

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
    tags: List[Tag],
    owner: String,
    copyright: LearningpathCopyright,
    learningsteps: List[LearningStep] = Nil
) extends Content

object LearningPathStatus extends Enumeration {
  val PUBLISHED, PRIVATE, DELETED, SUBMITTED, UNLISTED = Value
}

object LearningPathVerificationStatus extends Enumeration {
  val EXTERNAL, CREATED_BY_NDLA, VERIFIED_BY_NDLA = Value
}
