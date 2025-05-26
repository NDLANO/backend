/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class GrepSortDTO(override val entryName: String) extends EnumEntry
object GrepSortDTO extends Enum[GrepSortDTO] with CirceEnum[GrepSortDTO] {
  val values: IndexedSeq[GrepSortDTO] = findValues
  val all: Seq[String]                = values.map(_.entryName)

  case object ByRelevanceDesc extends GrepSortDTO("-relevance")
  case object ByRelevanceAsc  extends GrepSortDTO("relevance")
  case object ByTitleDesc     extends GrepSortDTO("-title")
  case object ByTitleAsc      extends GrepSortDTO("title")
  case object ByCodeDesc      extends GrepSortDTO("-code")
  case object ByCodeAsc       extends GrepSortDTO("code")
  case object ByStatusDesc    extends GrepSortDTO("-status")
  case object ByStatusAsc     extends GrepSortDTO("status")

  implicit val schema: Schema[GrepSortDTO]    = schemaForEnumEntry[GrepSortDTO]
  implicit val codec: PlainCodec[GrepSortDTO] = plainCodecEnumEntry[GrepSortDTO]
}
