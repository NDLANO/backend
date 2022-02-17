/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.language.model.LanguageField

case class ConceptMetaImage(imageId: String, altText: String, language: String)
    extends LanguageField[(String, String)] {
  override def isEmpty: Boolean        = imageId.isEmpty && altText.isEmpty
  override def value: (String, String) = imageId -> altText
}
