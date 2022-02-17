/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import no.ndla.language.model.LanguageField

case class VisualElement(resource: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = resource.isEmpty
  override def value: String    = resource
}
