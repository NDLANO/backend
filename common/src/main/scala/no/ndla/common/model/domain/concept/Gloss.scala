/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.concept

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class GlossExample(example: String, language: String, transcriptions: Map[String, String])

object GlossExample {
  implicit val encoder: Encoder[GlossExample] = deriveEncoder
  implicit val decoder: Decoder[GlossExample] = deriveDecoder
}

case class GlossData(
    gloss: String,
    wordClass: List[WordClass],
    originalLanguage: String,
    transcriptions: Map[String, String],
    examples: List[List[GlossExample]],
)

object GlossData {
  implicit val encoder: Encoder[GlossData] = deriveEncoder
  implicit val decoder: Decoder[GlossData] = deriveDecoder
}
