/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class Introduction(introduction: String, language: String) extends LanguageField[String] {
  override def value: String    = introduction
  override def isEmpty: Boolean = introduction.isEmpty
}

object Introduction {
  implicit val encoder: Encoder[Introduction] = deriveEncoder
  implicit val decoder: Decoder[Introduction] = deriveDecoder
}
