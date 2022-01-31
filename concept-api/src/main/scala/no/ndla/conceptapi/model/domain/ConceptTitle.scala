/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.language.model.LanguageField

case class ConceptTitle(title: String, language: String) extends LanguageField[String] {
  override def value: String = title
  override def isEmpty: Boolean = title.isEmpty
}
