/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import enumeratum.*

sealed trait LearningPathVerificationStatus extends EnumEntry {}
object LearningPathVerificationStatus
    extends Enum[LearningPathVerificationStatus]
    with CirceEnum[LearningPathVerificationStatus] {

  case object EXTERNAL         extends LearningPathVerificationStatus
  case object CREATED_BY_NDLA  extends LearningPathVerificationStatus
  case object VERIFIED_BY_NDLA extends LearningPathVerificationStatus

  override def values: IndexedSeq[LearningPathVerificationStatus] = findValues

  def valueOf(s: String): Option[LearningPathVerificationStatus] = {
    LearningPathVerificationStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrDefault(s: String): LearningPathVerificationStatus = {
    valueOf(s).getOrElse(LearningPathVerificationStatus.EXTERNAL)
  }
}
