/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

object ArticleStatus extends Enumeration {

  val IMPORTED, DRAFT, PUBLISHED, PROPOSAL, QUEUED_FOR_PUBLISHING, USER_TEST, AWAITING_QUALITY_ASSURANCE,
  QUALITY_ASSURED, AWAITING_UNPUBLISHING, UNPUBLISHED, ARCHIVED = Value

  def valueOf(s: String): Option[ArticleStatus.Value] = values.find(_.toString == s.toUpperCase)
}
