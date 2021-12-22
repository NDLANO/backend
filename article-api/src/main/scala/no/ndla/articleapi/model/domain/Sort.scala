/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

object Sort extends Enumeration {
  val ByRelevanceDesc = Value("-relevance")
  val ByRelevanceAsc = Value("relevance")
  val ByTitleDesc = Value("-title")
  val ByTitleAsc = Value("title")
  val ByLastUpdatedDesc = Value("-lastUpdated")
  val ByLastUpdatedAsc = Value("lastUpdated")
  val ByIdDesc = Value("-id")
  val ByIdAsc = Value("id")

  def valueOf(s: String): Option[Sort.Value] = Sort.values.find(_.toString == s)

}
