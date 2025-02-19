/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about the gloss example")
case class GlossExampleDTO(
    @description("Example use of the gloss") example: String,
    @description("Language of the example") language: String,
    @description("Alternative writing of the example") transcriptions: Map[String, String]
)

object GlossExampleDTO {
  implicit val encoder: Encoder[GlossExampleDTO] = deriveEncoder
  implicit val decoder: Decoder[GlossExampleDTO] = deriveDecoder
}

@description("Information about the gloss data")
case class GlossDataDTO(
    @description("The gloss itself") gloss: String,
    @description("Word class / part of speech, ex. noun, adjective, verb, adverb, ...") wordClass: String,
    @description("Original language of the gloss") originalLanguage: String,
    @description("Alternative writing of the gloss") transcriptions: Map[String, String],
    @description("List of examples of how the gloss can be used") examples: List[List[GlossExampleDTO]]
)

object GlossDataDTO {
  implicit val encoder: Encoder[GlossDataDTO] = deriveEncoder
  implicit val decoder: Decoder[GlossDataDTO] = deriveDecoder
}
