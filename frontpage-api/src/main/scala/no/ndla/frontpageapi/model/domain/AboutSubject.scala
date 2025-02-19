/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.domain

import no.ndla.language.model.LanguageField

case class AboutSubject(title: String, description: String, language: String, visualElement: VisualElement)
    extends LanguageField[(String, String, VisualElement)] {
  override def value: (String, String, VisualElement) = (title, description, visualElement)
  override def isEmpty: Boolean =
    title.isEmpty && description.isEmpty && visualElement.id.isEmpty && visualElement.alt.isEmpty
}
