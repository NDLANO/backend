/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

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
  implicit val schema: Schema[TitleWithHtmlDTO]   = DeriveHelpers.getSchema
}
