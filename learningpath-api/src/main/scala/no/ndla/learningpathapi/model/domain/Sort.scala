/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import com.scalatsi.TypescriptType.TSEnum
import com.scalatsi.{TSNamedType, TSType}
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
  case object ByDurationDesc    extends Sort("-duration")
  case object ByDurationAsc     extends Sort("duration")
  case object ByTitleDesc       extends Sort("-title")
  case object ByTitleAsc        extends Sort("title")

  def valueOf(s: String): Option[Sort] = Sort.values.find(_.entryName == s)

  def valueOf(s: Option[String]): Option[Sort] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }

  implicit val schema: Schema[Sort]               = schemaForEnumEntry[Sort]
  implicit val codec: PlainCodec[Sort]            = plainCodecEnumEntry[Sort]
  private val tsEnumValues: Seq[(String, String)] = values.map(e => e.toString -> e.entryName)
  implicit val enumTsType: TSNamedType[Sort] = TSType.alias[Sort](
    "Sort",
    TSEnum.string("LearningpathSortEnum", tsEnumValues: _*)
  )

}
