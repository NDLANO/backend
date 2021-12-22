/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

case class ArticleIntroduction(introduction: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = introduction.isEmpty
}
