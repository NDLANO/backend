/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain.concept

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class ConceptMetaImage(imageId: String, altText: String, language: String)
    extends LanguageField[(String, String)] {
  override def isEmpty: Boolean        = imageId.isEmpty && altText.isEmpty
  override def value: (String, String) = imageId -> altText
}

object ConceptMetaImage {
  implicit val encoder: Encoder[ConceptMetaImage] = deriveEncoder
  implicit val decoder: Decoder[ConceptMetaImage] = deriveDecoder
}
