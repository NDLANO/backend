/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.draft

object ArticleStatus extends Enumeration {

  val IMPORTED, DRAFT, PUBLISHED, PROPOSAL, QUEUED_FOR_PUBLISHING, USER_TEST, AWAITING_QUALITY_ASSURANCE,
  QUEUED_FOR_LANGUAGE, TRANSLATED, QUALITY_ASSURED, QUALITY_ASSURED_DELAYED, QUEUED_FOR_PUBLISHING_DELAYED,
  AWAITING_UNPUBLISHING, UNPUBLISHED, AWAITING_ARCHIVING, ARCHIVED = Value

  def valueOf(s: String): Option[ArticleStatus.Value] = values.find(_.toString == s.toUpperCase)
}
