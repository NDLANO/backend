/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.language.model.LanguageField

case class ArticleTitle(title: String, language: String) extends LanguageField[String] {
  override def value: String = title
  override def isEmpty: Boolean = title.isEmpty
}
