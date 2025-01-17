/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.language.model.LanguageField

case class Disclaimer(disclaimer: String, language: String) extends LanguageField[String] {
  override def value: String    = disclaimer
  override def isEmpty: Boolean = disclaimer.isEmpty
}

object Disclaimer {
  implicit def encoder: Encoder[Disclaimer] = deriveEncoder[Disclaimer]
  implicit def decoder: Decoder[Disclaimer] = deriveDecoder[Disclaimer]
}
