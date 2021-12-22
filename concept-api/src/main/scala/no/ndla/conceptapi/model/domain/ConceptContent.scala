/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class ConceptContent(content: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = content.isEmpty
}
