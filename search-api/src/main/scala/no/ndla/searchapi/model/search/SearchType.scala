/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

object SearchType extends Enumeration {
  val Articles: SearchType.Value = Value("article")
  val Drafts: SearchType.Value = Value("draft")
  val LearningPaths: SearchType.Value = Value("learningpath")

  def all: List[String] = SearchType.values.map(_.toString).toList

}
