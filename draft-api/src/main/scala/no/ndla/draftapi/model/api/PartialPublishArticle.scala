/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import enumeratum._
import sttp.tapir.Schema.annotations.description

sealed trait PartialArticleFieldsDTO extends EnumEntry

object PartialArticleFieldsDTO extends Enum[PartialArticleFieldsDTO] with CirceEnum[PartialArticleFieldsDTO] {
  override val values: IndexedSeq[PartialArticleFieldsDTO] = findValues

  case object availability    extends PartialArticleFieldsDTO
  case object grepCodes       extends PartialArticleFieldsDTO
  case object license         extends PartialArticleFieldsDTO
  case object metaDescription extends PartialArticleFieldsDTO
  case object relatedContent  extends PartialArticleFieldsDTO
  case object tags            extends PartialArticleFieldsDTO
  case object revisionDate    extends PartialArticleFieldsDTO
  case object published       extends PartialArticleFieldsDTO
}

@description("Partial data about articles to publish in bulk")
case class PartialBulkArticlesDTO(
    @description("A list of article ids to partially publish") articleIds: Seq[Long],
    @description("A list of fields that should be partially published") fields: Seq[PartialArticleFieldsDTO]
)
