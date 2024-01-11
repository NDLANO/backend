/*
 * Part of NDLA common.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class Introduction(introduction: String, language: String) extends LanguageField[String] {
  override def value: String    = introduction
  override def isEmpty: Boolean = introduction.isEmpty
}

object Introduction {
  implicit def encoder: Encoder[Introduction] = deriveEncoder[Introduction]
  implicit def decoder: Decoder[Introduction] = deriveDecoder[Introduction]
}
