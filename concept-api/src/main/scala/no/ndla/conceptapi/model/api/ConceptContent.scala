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
case class ConceptContent(
    @description("The content of this concept") content: String,
    @description("The html content of this concept") htmlContent: String,
    @description("The language of this concept") language: String
)

object ConceptContent {
  implicit val encoder: Encoder[ConceptContent] = deriveEncoder
  implicit val decoder: Decoder[ConceptContent] = deriveDecoder
}
