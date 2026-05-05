/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import enumeratum.*
import sttp.tapir.Codec.PlainCodec
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class Sort(override val entryName: String) extends EnumEntry

object Sort extends Enum[Sort] with CirceEnum[Sort] {

  val values: IndexedSeq[Sort] = findValues

  val all: Seq[String] = values.map(_.entryName)

  case object ByIdDesc          extends Sort("-id")
  case object ByIdAsc           extends Sort("id")
  case object ByRelevanceDesc   extends Sort("-relevance")
  case object ByRelevanceAsc    extends Sort("relevance")
  case object ByLastUpdatedDesc extends Sort("-lastUpdated")
  case object ByLastUpdatedAsc  extends Sort("lastUpdated")
  case object ByTitleDesc       extends Sort("-title")
  case object ByTitleAsc        extends Sort("title")
  case object ByCreatedDesc     extends Sort("-created")
  case object ByCreatedAsc      extends Sort("created")

  def valueOf(s: String): Option[Sort] = Sort.values.find(_.entryName == s)

  def valueOf(s: Option[String]): Option[Sort] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

  implicit val schema: Schema[Sort]    = schemaForEnumEntry[Sort]
  implicit val codec: PlainCodec[Sort] = plainCodecEnumEntry[Sort]
}
