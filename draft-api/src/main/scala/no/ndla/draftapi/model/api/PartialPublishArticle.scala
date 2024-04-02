/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import enumeratum._
import sttp.tapir.Schema.annotations.description

sealed trait PartialArticleFields extends EnumEntry

object PartialArticleFields extends Enum[PartialArticleFields] with CirceEnum[PartialArticleFields] {
  override val values: IndexedSeq[PartialArticleFields] = findValues

  case object availability    extends PartialArticleFields
  case object grepCodes       extends PartialArticleFields
  case object license         extends PartialArticleFields
  case object metaDescription extends PartialArticleFields
  case object relatedContent  extends PartialArticleFields
  case object tags            extends PartialArticleFields
  case object revisionDate    extends PartialArticleFields
}

// format: off
@description("Partial data about articles to publish in bulk")
case class PartialBulkArticles(
    @description("A list of article ids to partially publish") articleIds: Seq[Long],
    @description("A list of fields that should be partially published") fields: Seq[PartialArticleFields],
)
