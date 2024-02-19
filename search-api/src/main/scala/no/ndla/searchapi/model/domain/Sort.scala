/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

import enumeratum._

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
  case object ByDurationDesc               extends Sort("-duration")
  case object ByDurationAsc                extends Sort("duration")
  case object ByRevisionDateAsc            extends Sort("revisionDate")
  case object ByRevisionDateDesc           extends Sort("-revisionDate")
  case object ByResponsibleLastUpdatedAsc  extends Sort("responsibleLastUpdated")
  case object ByResponsibleLastUpdatedDesc extends Sort("-responsibleLastUpdated")
  case object ByStatusAsc                  extends Sort("status")
  case object ByStatusDesc                 extends Sort("-status")
  case object ByPrioritizedDesc            extends Sort("-prioritized")
  case object ByPrioritizedAsc             extends Sort("prioritized")
  case object ByParentTopicNameDesc        extends Sort("-parentTopicName")
  case object ByParentTopicNameAsc         extends Sort("parentTopicName")
  case object ByPrimaryRootDesc            extends Sort("-primaryRoot")
  case object ByPrimaryRootAsc             extends Sort("primaryRoot")
  case object ByResourceTypeDesc           extends Sort("-resourceType")
  case object ByResourceTypeAsc            extends Sort("resourceType")

  def valueOf(s: String): Option[Sort] = Sort.values.find(_.entryName == s)
}
