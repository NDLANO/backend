/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
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

  implicit val schema: Schema[GrepSortDTO]    = schemaForEnumEntry[GrepSortDTO]
  implicit val codec: PlainCodec[GrepSortDTO] = plainCodecEnumEntry[GrepSortDTO]
  implicit val enumTsType: TSNamedType[GrepSortDTO] =
    TSType.alias[GrepSortDTO]("GrepSort", TSUnion(all.map(s => TSLiteralString(s))))

}
