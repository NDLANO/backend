/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import enumeratum.*
import no.ndla.common.errors.{ValidationException, ValidationMessage}

import scala.util.{Failure, Success, Try}

sealed trait LearningPathStatus extends EnumEntry {}
object LearningPathStatus extends Enum[LearningPathStatus] with CirceEnum[LearningPathStatus] {

  case object PUBLISHED extends LearningPathStatus
  case object PRIVATE   extends LearningPathStatus
  case object DELETED   extends LearningPathStatus
  case object UNLISTED  extends LearningPathStatus
  case object SUBMITTED extends LearningPathStatus

  override def values: IndexedSeq[LearningPathStatus] = findValues

  def valueOf(s: String): Option[LearningPathStatus] = {
    LearningPathStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOfOrError(status: String): Try[LearningPathStatus] = {
    valueOf(status) match {
      case Some(status) => Success(status)
      case None =>
        Failure(
          new ValidationException(
            errors = List(ValidationMessage("status", s"'$status' is not a valid publishingstatus."))
          )
        )
    }

  }

  def valueOfOrDefault(s: String): LearningPathStatus = {
    valueOf(s).getOrElse(LearningPathStatus.PRIVATE)
  }
}
