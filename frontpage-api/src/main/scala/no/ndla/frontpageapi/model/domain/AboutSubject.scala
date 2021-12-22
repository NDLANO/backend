/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.model.domain

case class AboutSubject(title: String, description: String, language: String, visualElement: VisualElement)
    extends LanguageField {
  override def isEmpty: Boolean = description.isEmpty
}
