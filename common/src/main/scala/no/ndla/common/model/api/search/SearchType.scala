/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

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
  case object Nodes         extends SearchType("node")

  def all: List[String]                       = SearchType.values.map(_.toString).toList
  override def values: IndexedSeq[SearchType] = findValues

  implicit def schema: Schema[SearchType] = schemaForEnumEntry[SearchType]
}
