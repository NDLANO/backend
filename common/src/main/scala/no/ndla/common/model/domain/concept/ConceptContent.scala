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

case class ConceptContent(content: String, language: String) extends LanguageField[String] {
  override def value: String    = content
  override def isEmpty: Boolean = content.isEmpty
}

object ConceptContent {
  implicit val encoder: Encoder[ConceptContent] = deriveEncoder
  implicit val decoder: Decoder[ConceptContent] = deriveDecoder
}
