/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.enumeratum.*
import no.ndla.common.DeriveHelpers

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

  implicit def schema: Schema[PartialArticleFieldsDTO]         = schemaForEnumEntry[PartialArticleFieldsDTO]
  implicit def seqSchema: Schema[Seq[PartialArticleFieldsDTO]] = schema.asIterable
  implicit def codec: PlainCodec[PartialArticleFieldsDTO]      = plainCodecEnumEntry[PartialArticleFieldsDTO]
}

@description("Partial data about articles to publish in bulk")
case class PartialBulkArticlesDTO(
    @description("A list of article ids to partially publish")
    articleIds: Seq[Long],
    @description("A list of fields that should be partially published")
    fields: Seq[PartialArticleFieldsDTO],
)

object PartialBulkArticlesDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit def encoder: Encoder[PartialBulkArticlesDTO] = deriveEncoder
  implicit def decoder: Decoder[PartialBulkArticlesDTO] = deriveDecoder
  implicit def schema: Schema[PartialBulkArticlesDTO]   = DeriveHelpers.getSchema
}
