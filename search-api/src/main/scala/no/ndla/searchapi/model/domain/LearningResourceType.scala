/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.domain

import enumeratum.*

sealed abstract class LearningResourceType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object LearningResourceType extends Enum[LearningResourceType] with CirceEnum[LearningResourceType] {
  case object Article      extends LearningResourceType("standard")
  case object TopicArticle extends LearningResourceType("topic-article")
  case object LearningPath extends LearningResourceType("learningpath")
  case object Concept      extends LearningResourceType("concept")

  def all: List[String]                                 = LearningResourceType.values.map(_.entryName).toList
  def valueOf(s: String): Option[LearningResourceType]  = LearningResourceType.values.find(_.entryName == s)
  override def values: IndexedSeq[LearningResourceType] = findValues
}
