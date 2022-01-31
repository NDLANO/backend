/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.language.model.LanguageField

case class VisualElement(visualElement: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = visualElement.isEmpty
  override def value: String = visualElement
}
