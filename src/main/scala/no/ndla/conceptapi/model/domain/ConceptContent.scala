/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class ConceptContent(content: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = content.isEmpty
}
