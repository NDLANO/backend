/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField
import sttp.tapir.Schema.annotations.description

@description("The description of the learningpath")
case class Description(
    @description("The learningpath description. Basic HTML allowed") description: String,
    @description("ISO 639-1 code that represents the language used in description") language: String
) extends LanguageField[String] {
  override def value: String    = description
  override def isEmpty: Boolean = description.isEmpty
}

object Description {
  implicit val encoder: Encoder[Description] = deriveEncoder
  implicit val decoder: Decoder[Description] = deriveDecoder
}
