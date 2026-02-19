/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import enumeratum.*
import no.ndla.common.model.domain.ArticleType
import no.ndla.common.model.domain.concept.ConceptType
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class LearningResourceType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object LearningResourceType extends Enum[LearningResourceType] with CirceEnum[LearningResourceType] {
  case object Article          extends LearningResourceType("standard")
  case object TopicArticle     extends LearningResourceType("topic-article")
  case object FrontpageArticle extends LearningResourceType("frontpage-article")
  case object CaseArticle      extends LearningResourceType("case-article")
  case object LearningPath     extends LearningResourceType("learningpath")
  case object Concept          extends LearningResourceType("concept")
  case object Gloss            extends LearningResourceType("gloss")

  def all: List[String]                                 = LearningResourceType.values.map(_.entryName).toList
  def valueOf(s: String): Option[LearningResourceType]  = LearningResourceType.values.find(_.entryName == s)
  override def values: IndexedSeq[LearningResourceType] = findValues

  implicit def schema: Schema[LearningResourceType] = schemaForEnumEntry[LearningResourceType]

  def fromArticleType(articleType: ArticleType): LearningResourceType = {
    articleType match {
      case ArticleType.Standard         => Article
      case ArticleType.TopicArticle     => TopicArticle
      case ArticleType.FrontpageArticle => FrontpageArticle
      case ArticleType.CaseArticle      => CaseArticle
    }
  }

  def fromConceptType(conceptType: ConceptType): LearningResourceType = {
    conceptType match {
      case ConceptType.CONCEPT => LearningResourceType.Concept
      case ConceptType.GLOSS   => LearningResourceType.Gloss
    }

  }
}
