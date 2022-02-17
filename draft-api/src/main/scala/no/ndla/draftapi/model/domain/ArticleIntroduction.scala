/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.language.model.LanguageField

case class ArticleIntroduction(introduction: String, language: String) extends LanguageField[String] {
  override def value: String    = introduction
  override def isEmpty: Boolean = introduction.isEmpty
}
