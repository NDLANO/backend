/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class ConceptTitle(title: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = title.isEmpty
}
