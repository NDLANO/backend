/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

object Sort extends Enumeration {
  val ByRelevanceDesc = Value("-relevance")
  val ByRelevanceAsc = Value("relevance")
  val ByTitleDesc = Value("-title")
  val ByTitleAsc = Value("title")
  val ByLastUpdatedDesc = Value("-lastUpdated")
  val ByLastUpdatedAsc = Value("lastUpdated")
  val ByIdDesc = Value("-id")
  val ByIdAsc = Value("id")
  val ByDurationDesc = Value("-duration")
  val ByDurationAsc = Value("duration")

  def valueOf(s: String): Option[Sort.Value] = Sort.values.find(_.toString == s)

}
