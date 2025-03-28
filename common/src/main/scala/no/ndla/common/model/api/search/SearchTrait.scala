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

sealed abstract class SearchTrait(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}
object SearchTrait extends Enum[SearchTrait] with CirceEnumWithErrors[SearchTrait] {
  case object Video   extends SearchTrait("VIDEO")
  case object H5p     extends SearchTrait("H5P")
  case object Audio   extends SearchTrait("AUDIO")
  case object Podcast extends SearchTrait("PODCAST")

  def all: List[String]                        = SearchTrait.values.map(_.toString).toList
  override def values: IndexedSeq[SearchTrait] = findValues
  def valueOf(s: String): Option[SearchTrait]  = SearchTrait.values.find(_.entryName == s)
  implicit def schema: Schema[SearchTrait]     = schemaForEnumEntry[SearchTrait]
}
