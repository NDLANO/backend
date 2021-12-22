/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.domain

case class ArticleMetaImage(imageId: String, altText: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = imageId.isEmpty && altText.isEmpty
  override def value: String = imageId
}
