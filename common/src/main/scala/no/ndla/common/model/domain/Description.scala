/*
 * Part of NDLA common
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class Description(content: String, language: String) extends LanguageField[String] {
  override def value: String    = content
  override def isEmpty: Boolean = content.isEmpty
}

object Description {
  implicit def encoder: Encoder[Description] = deriveEncoder[Description]
  implicit def decoder: Decoder[Description] = deriveDecoder[Description]
}
