/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import enumeratum.*
import no.ndla.common.errors.{ValidationException, ValidationMessage}

sealed abstract class StepStatus(override val entryName: String) extends EnumEntry
object StepStatus                                                extends Enum[StepStatus] with CirceEnum[StepStatus] {

  case object ACTIVE   extends StepStatus("ACTIVE")
  case object MIGRATED extends StepStatus("MIGRATED")
  case object DELETED  extends StepStatus("DELETED")

  def values: IndexedSeq[StepStatus] = findValues

  def valueOf(s: String): Option[StepStatus] = {
    StepStatus.values.find(_.entryName == s)
  }

  def valueOfOrError(status: String): StepStatus = {
    valueOf(status) match {
      case Some(s) => s
      case None    =>
        throw new ValidationException(errors = List(ValidationMessage("status", s"'$status' is not a valid status.")))
    }
  }
}
