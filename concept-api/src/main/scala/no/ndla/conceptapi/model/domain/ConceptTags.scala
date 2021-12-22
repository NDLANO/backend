/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class ConceptTags(tags: Seq[String], language: String) extends LanguageField {
  override def isEmpty: Boolean = tags.isEmpty
}
