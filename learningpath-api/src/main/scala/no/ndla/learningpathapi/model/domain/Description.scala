/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

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
