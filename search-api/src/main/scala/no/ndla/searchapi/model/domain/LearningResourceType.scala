/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

object LearningResourceType extends Enumeration {
  val Article: LearningResourceType.Value      = Value("standard")
  val TopicArticle: LearningResourceType.Value = Value("topic-article")
  val LearningPath: LearningResourceType.Value = Value("learningpath")

  def all: List[String] = LearningResourceType.values.map(_.toString).toList

  def valueOf(s: String): Option[LearningResourceType.Value] = LearningResourceType.values.find(_.toString == s)
}
