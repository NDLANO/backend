/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about the concept")
case class ConceptTitleDTO(
    @description("The freetext title of this concept")
    title: String,
    @description("The freetext html title of this concept")
    htmlTitle: String,
    @description("ISO 639-1 code that represents the language used in the title")
    language: String,
)

object ConceptTitleDTO {
  implicit val encoder: Encoder[ConceptTitleDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptTitleDTO] = deriveDecoder
}
