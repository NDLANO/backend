/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleMetaDescription(content: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = content.isEmpty
  override def value: String = content
}
