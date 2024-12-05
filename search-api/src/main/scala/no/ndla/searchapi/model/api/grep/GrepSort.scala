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

sealed abstract class GrepSort(override val entryName: String) extends EnumEntry
object GrepSort extends Enum[GrepSort] with CirceEnum[GrepSort] {
  val values: IndexedSeq[GrepSort] = findValues
  val all: Seq[String]             = values.map(_.entryName)

  case object ByRelevanceDesc extends GrepSort("-relevance")
  case object ByRelevanceAsc  extends GrepSort("relevance")
  case object ByTitleDesc     extends GrepSort("-title")
  case object ByTitleAsc      extends GrepSort("title")
  case object ByCodeDesc      extends GrepSort("-code")
  case object ByCodeAsc       extends GrepSort("code")

  implicit val schema: Schema[GrepSort]    = schemaForEnumEntry[GrepSort]
  implicit val codec: PlainCodec[GrepSort] = plainCodecEnumEntry[GrepSort]
  implicit val enumTsType: TSNamedType[GrepSort] =
    TSType.alias[GrepSort]("GrepSort", TSUnion(all.map(s => TSLiteralString(s))))

}
