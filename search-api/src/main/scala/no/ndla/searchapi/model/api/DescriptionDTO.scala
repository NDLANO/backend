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
case class DescriptionDTO(
    @description("The freetext description of the resource") description: String,
    @description("ISO 639-1 code that represents the language used in title") language: String
) extends LanguageField[String] {
  override def value: String    = description
  override def isEmpty: Boolean = description.isEmpty
}

object DescriptionDTO {
  implicit val encoder: Encoder[DescriptionDTO] = deriveEncoder
  implicit val decoder: Decoder[DescriptionDTO] = deriveDecoder
}
