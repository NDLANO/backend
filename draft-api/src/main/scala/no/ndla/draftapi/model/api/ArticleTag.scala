/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Description of the tags of the article")
case class ArticleTag(
    @description("The searchable tag.") tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag") language: String
)

object ArticleTag {
  implicit def encoder: Encoder[ArticleTag] = deriveEncoder
  implicit def decoder: Decoder[ArticleTag] = deriveDecoder
}
