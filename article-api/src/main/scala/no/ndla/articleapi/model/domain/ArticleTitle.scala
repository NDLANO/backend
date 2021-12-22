/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleTitle(title: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = title.isEmpty
  override def value: String = title
}
