/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.*
import no.ndla.common.CirceUtil.CirceEnumWithErrors
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class SearchType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}
object SearchType extends Enum[SearchType] with CirceEnumWithErrors[SearchType] {
  case object Articles      extends SearchType("article")
  case object Drafts        extends SearchType("draft")
  case object LearningPaths extends SearchType("learningpath")
  case object Concepts      extends SearchType("concept")
  case object Grep          extends SearchType("grep")

  def all: List[String]                       = SearchType.values.map(_.toString).toList
  override def values: IndexedSeq[SearchType] = findValues

  implicit def schema: Schema[SearchType] = schemaForEnumEntry[SearchType]

  implicit val enumTsType: TSNamedType[SearchType] =
    TSType.alias[SearchType]("SearchType", TSUnion(values.map(e => TSLiteralString(e.entryName))))
}
