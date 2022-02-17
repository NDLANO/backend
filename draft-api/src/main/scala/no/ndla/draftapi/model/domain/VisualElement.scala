/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.language.model.LanguageField

case class VisualElement(resource: String, language: String) extends LanguageField[String] {
  override def value: String    = resource
  override def isEmpty: Boolean = resource.isEmpty
}
