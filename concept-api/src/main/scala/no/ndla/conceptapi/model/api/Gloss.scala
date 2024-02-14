/*
 * Part of NDLA concept-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Information about the gloss example")
case class GlossExample(
    @(ApiModelProperty @field)(description = "Example use of the gloss") example: String,
    @(ApiModelProperty @field)(description = "Language of the example") language: String,
    @(ApiModelProperty @field)(description = "Alternative writing of the example") transcriptions: Map[String, String],
)

object GlossExample {
  implicit val encoder: Encoder[GlossExample] = deriveEncoder
  implicit val decoder: Decoder[GlossExample] = deriveDecoder
}

@ApiModel(description = "Information about the gloss data")
case class GlossData(
    @(ApiModelProperty @field)(description = "The gloss itself") gloss: String,
    @(ApiModelProperty @field)(description = "Word class / part of speech, ex. noun, adjective, verb, adverb, ...") wordClass: String,
    @(ApiModelProperty @field)(description = "Original language of the gloss") originalLanguage: String,
    @(ApiModelProperty @field)(description = "Alternative writing of the gloss") transcriptions: Map[String, String],
    @(ApiModelProperty @field)(description = "List of examples of how the gloss can be used") examples: List[List[GlossExample]],
)
// format: on

object GlossData {
  implicit val encoder: Encoder[GlossData] = deriveEncoder
  implicit val decoder: Decoder[GlossData] = deriveDecoder
}
