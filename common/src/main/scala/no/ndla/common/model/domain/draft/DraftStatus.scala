/*
 * Part of NDLA common.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}

object DraftStatus extends Enumeration {

  val IMPORTED, PLANNED, IN_PROGRESS, EXTERNAL_REVIEW, INTERNAL_REVIEW, QUALITY_ASSURANCE, LANGUAGE, FOR_APPROVAL,
      END_CONTROL, PUBLISHED, UNPUBLISHED, ARCHIVED = Value

  def valueOfOrError(s: String): Try[DraftStatus.Value] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          ValidationException("status", s"'$s' is not a valid article status. Must be one of $validStatuses")
        )
    }

  def valueOf(s: String): Option[DraftStatus.Value] = values.find(_.toString == s.toUpperCase)
}
