/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.concept

import enumeratum.*
import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}

sealed trait ConceptStatus extends EnumEntry                                         {}
object ConceptStatus       extends Enum[ConceptStatus] with CirceEnum[ConceptStatus] {
  case object IN_PROGRESS       extends ConceptStatus
  case object EXTERNAL_REVIEW   extends ConceptStatus
  case object INTERNAL_REVIEW   extends ConceptStatus
  case object QUALITY_ASSURANCE extends ConceptStatus
  case object LANGUAGE          extends ConceptStatus
  case object FOR_APPROVAL      extends ConceptStatus
  case object END_CONTROL       extends ConceptStatus
  case object PUBLISHED         extends ConceptStatus
  case object UNPUBLISHED       extends ConceptStatus
  case object ARCHIVED          extends ConceptStatus

  val values: IndexedSeq[ConceptStatus] = findValues

  def valueOfOrError(s: String): Try[ConceptStatus] = valueOf(s) match {
    case Some(st) => Success(st)
    case None     =>
      val validStatuses = values.map(_.toString).mkString(", ")
      Failure(ValidationException("status", s"'$s' is not a valid concept status. Must be one of $validStatuses"))
  }

  def valueOf(s: String): Option[ConceptStatus] = values.find(_.toString == s.toUpperCase)

  val thatDoesNotRequireResponsible: Seq[ConceptStatus] = Seq(PUBLISHED, UNPUBLISHED, ARCHIVED)
  val thatRequiresResponsible: Set[ConceptStatus]       = this.values.filterNot(thatDoesNotRequireResponsible.contains).toSet

  implicit def ordering[A <: ConceptStatus]: Ordering[ConceptStatus] =
    (x: ConceptStatus, y: ConceptStatus) => indexOf(x) - indexOf(y)
}
