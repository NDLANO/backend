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

@description("Representation of a title")
case class TitleDTO(
    @description("The title of the content. Must be plain text")
    title: String,
    @description("ISO 639-1 code that represents the language used in title")
    language: String,
) extends LanguageField[String] {
  override def value: String    = title
  override def isEmpty: Boolean = title.isEmpty
}

object TitleDTO {
  implicit val encoder: Encoder[TitleDTO] = deriveEncoder
  implicit val decoder: Decoder[TitleDTO] = deriveDecoder
}
