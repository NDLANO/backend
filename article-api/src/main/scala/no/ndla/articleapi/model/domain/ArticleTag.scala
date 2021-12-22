/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleTag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def isEmpty: Boolean = tags.isEmpty
  override def value: Seq[String] = tags
}
