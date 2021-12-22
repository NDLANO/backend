/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

case class ConceptMetaImage(imageId: String, altText: String, language: String) extends LanguageField {
  override def isEmpty: Boolean = imageId.isEmpty && altText.isEmpty

}
