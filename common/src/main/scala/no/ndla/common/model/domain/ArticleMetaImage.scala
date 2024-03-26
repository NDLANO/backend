/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class ArticleMetaImage(imageId: String, altText: String, language: String)
    extends LanguageField[(String, String)] {
  override def value: (String, String) = imageId -> altText
  override def isEmpty: Boolean        = imageId.isEmpty && altText.isEmpty
}

object ArticleMetaImage {
  implicit def encoder: Encoder[ArticleMetaImage] = deriveEncoder[ArticleMetaImage]
  implicit def decoder: Decoder[ArticleMetaImage] = deriveDecoder[ArticleMetaImage]
}
