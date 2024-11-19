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

case class Description(description: String, language: String) extends LanguageField[String] {
  override def value: String    = description
  override def isEmpty: Boolean = description.isEmpty
}

object Description {
  implicit val encoder: Encoder[Description] = deriveEncoder
  implicit val decoder: Decoder[Description] = deriveDecoder
}
