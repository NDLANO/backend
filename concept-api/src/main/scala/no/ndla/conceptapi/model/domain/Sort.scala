/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.domain

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

  case object ByRelevanceDesc              extends Sort("-relevance")
  case object ByRelevanceAsc               extends Sort("relevance")
  case object ByTitleDesc                  extends Sort("-title")
  case object ByTitleAsc                   extends Sort("title")
  case object ByLastUpdatedDesc            extends Sort("-lastUpdated")
  case object ByLastUpdatedAsc             extends Sort("lastUpdated")
  case object ByIdDesc                     extends Sort("-id")
  case object ByIdAsc                      extends Sort("id")
  case object ByResponsibleLastUpdatedDesc extends Sort("-responsibleLastUpdated")
  case object ByResponsibleLastUpdatedAsc  extends Sort("responsibleLastUpdated")
  case object ByStatusAsc                  extends Sort("status")
  case object ByStatusDesc                 extends Sort("-status")
  case object BySubjectAsc                 extends Sort("subject")
  case object BySubjectDesc                extends Sort("-subject")
  case object ByConceptTypeAsc             extends Sort("conceptType")
  case object ByConceptTypeDesc            extends Sort("-conceptType")

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
    TSEnum.string("ConceptSortEnum", tsEnumValues: _*)
  )
}
