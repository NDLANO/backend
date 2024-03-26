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
import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

case class LearningPathTags(
    @description("The searchable tags. Must be plain text") tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag") language: String
) extends WithLanguage

object LearningPathTags {
  implicit val encoder: Encoder[LearningPathTags] = deriveEncoder
  implicit val decoder: Decoder[LearningPathTags] = deriveDecoder
}
