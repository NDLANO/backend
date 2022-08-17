/*
 * Part of NDLA common.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.draft

import no.ndla.common.errors.ValidationException

import scala.util.{Failure, Success, Try}

object ArticleStatus extends Enumeration {

  val IMPORTED, DRAFT, PUBLISHED, PROPOSAL, QUEUED_FOR_PUBLISHING, USER_TEST, AWAITING_QUALITY_ASSURANCE,
      QUEUED_FOR_LANGUAGE, TRANSLATED, QUALITY_ASSURED, QUALITY_ASSURED_DELAYED, QUEUED_FOR_PUBLISHING_DELAYED,
      AWAITING_UNPUBLISHING, UNPUBLISHED, AWAITING_ARCHIVING, ARCHIVED = Value

  def valueOfOrError(s: String): Try[ArticleStatus.Value] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          ValidationException("status", s"'$s' is not a valid article status. Must be one of $validStatuses")
        )
    }

  def valueOf(s: String): Option[ArticleStatus.Value] = values.find(_.toString == s.toUpperCase)
}
