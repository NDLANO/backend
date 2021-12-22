/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

case class LearningPathTags(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
}
