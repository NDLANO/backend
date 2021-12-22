/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

case class ArticleIntroduction(introduction: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = introduction.isEmpty
  override def value: String = introduction
}
