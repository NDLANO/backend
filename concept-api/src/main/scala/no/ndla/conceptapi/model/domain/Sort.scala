/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import enumeratum._

sealed abstract class Sort(override val entryName: String) extends EnumEntry

object Sort extends Enum[Sort] {

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

}
