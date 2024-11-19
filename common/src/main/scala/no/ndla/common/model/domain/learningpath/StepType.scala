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

sealed trait StepType extends EnumEntry
object StepType extends Enum[StepType] with CirceEnum[StepType] {
  case object INTRODUCTION extends StepType
  case object TEXT         extends StepType
  case object QUIZ         extends StepType
  case object TASK         extends StepType
  case object MULTIMEDIA   extends StepType
  case object SUMMARY      extends StepType
  case object TEST         extends StepType

  def valueOf(s: String): Option[StepType]  = StepType.values.find(_.toString == s)
  def valueOfOrDefault(s: String): StepType = valueOf(s).getOrElse(StepType.TEXT)

  def valueOfOrError(s: String): StepType = {
    valueOf(s) match {
      case Some(stepType) => stepType
      case None =>
        throw new ValidationException(errors = List(ValidationMessage("type", s"'$s' is not a valid steptype.")))
    }
  }

  override def values: IndexedSeq[StepType] = findValues
}
