/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField
import sttp.tapir.Schema.annotations.description

@description("Title of resource")
case class TitleWithHtmlDTO(
    @description("The freetext title of the resource") title: String,
    @description("The freetext html-version title of the article") htmlTitle: String,
    @description("ISO 639-1 code that represents the language used in title") language: String
) extends LanguageField[String] {
  override def value: String    = title
  override def isEmpty: Boolean = title.isEmpty
}

object TitleWithHtmlDTO {
  implicit val encoder: Encoder[TitleWithHtmlDTO] = deriveEncoder
  implicit val decoder: Decoder[TitleWithHtmlDTO] = deriveDecoder
}
