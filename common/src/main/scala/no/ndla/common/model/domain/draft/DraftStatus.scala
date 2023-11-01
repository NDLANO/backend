/*
 * Part of NDLA common.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import enumeratum._
import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}

sealed trait DraftStatus extends EnumEntry {}

object DraftStatus extends Enum[DraftStatus] {
  case object IMPORTED          extends DraftStatus
  case object PLANNED           extends DraftStatus
  case object IN_PROGRESS       extends DraftStatus
  case object EXTERNAL_REVIEW   extends DraftStatus
  case object INTERNAL_REVIEW   extends DraftStatus
  case object QUALITY_ASSURANCE extends DraftStatus
  case object LANGUAGE          extends DraftStatus
  case object FOR_APPROVAL      extends DraftStatus
  case object END_CONTROL       extends DraftStatus
  case object PUBLISH_DELAYED   extends DraftStatus
  case object PUBLISHED         extends DraftStatus
  case object REPUBLISH         extends DraftStatus
  case object UNPUBLISHED       extends DraftStatus
  case object ARCHIVED          extends DraftStatus

  val values: IndexedSeq[DraftStatus] = findValues

  def valueOfOrError(s: String): Try[DraftStatus] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          ValidationException("status", s"'$s' is not a valid article status. Must be one of $validStatuses")
        )
    }

  def valueOf(s: String): Option[DraftStatus] = values.find(_.toString == s.toUpperCase)

  val thatDoesNotRequireResponsible: Seq[DraftStatus] = Seq(PUBLISHED, UNPUBLISHED, ARCHIVED)
  val thatRequiresResponsible: Seq[DraftStatus]       = this.values.filterNot(thatDoesNotRequireResponsible.contains)
}
