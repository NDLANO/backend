/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class MetaDescription(metaDescription: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = metaDescription.isEmpty
  override def value: String    = metaDescription
}

object MetaDescription {
  implicit val encoder: Encoder[MetaDescription] = deriveEncoder
  implicit val decoder: Decoder[MetaDescription] = deriveDecoder
}
