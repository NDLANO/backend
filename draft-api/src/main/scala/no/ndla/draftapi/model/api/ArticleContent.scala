/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("The content of the article in the specified language")
case class ArticleContent(
    @description("The html content") content: String,
    @description("ISO 639-1 code that represents the language used in the content") language: String
)

object ArticleContent {
  implicit def encoder: Encoder[ArticleContent] = deriveEncoder
  implicit def decoder: Decoder[ArticleContent] = deriveDecoder
}
