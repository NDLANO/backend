/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.language.model.LanguageField

case class ConceptContent(content: String, language: String) extends LanguageField[String] {
  override def value: String    = content
  override def isEmpty: Boolean = content.isEmpty
}

object ConceptContent {
  implicit val encoder: Encoder[ConceptContent] = deriveEncoder
  implicit val decoder: Decoder[ConceptContent] = deriveDecoder
}
