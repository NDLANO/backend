/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.language.model.LanguageField

case class ArticleMetaImage(imageId: String, altText: String, language: String)
    extends LanguageField[(String, String)] {
  override def value: (String, String) = imageId -> altText
  override def isEmpty: Boolean = imageId.isEmpty && altText.isEmpty
}
