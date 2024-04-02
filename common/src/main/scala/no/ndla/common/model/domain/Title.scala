/*
 * Part of NDLA common
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.language.model.LanguageField

case class Title(title: String, language: String) extends LanguageField[String] {
  override def value: String    = title
  override def isEmpty: Boolean = title.isEmpty
}

object Title {
  implicit def encoder: Encoder[Title] = deriveEncoder[Title]
  implicit def decoder: Decoder[Title] = deriveDecoder[Title]
}
