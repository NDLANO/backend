/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

case class VisualElement(resource: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = resource.isEmpty
}
