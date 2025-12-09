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

sealed abstract class ArticleTrait(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}
object ArticleTrait extends Enum[ArticleTrait] with CirceEnumWithErrors[ArticleTrait] {
  case object Video       extends ArticleTrait("VIDEO")
  case object Audio       extends ArticleTrait("AUDIO")
  case object Podcast     extends ArticleTrait("PODCAST")
  case object Interactive extends ArticleTrait("INTERACTIVE")

  def all: List[String]                         = ArticleTrait.values.map(_.toString).toList
  override def values: IndexedSeq[ArticleTrait] = findValues
  def valueOf(s: String): Option[ArticleTrait]  = ArticleTrait.values.find(_.entryName == s)
  implicit def schema: Schema[ArticleTrait]     = schemaForEnumEntry[ArticleTrait]
}
