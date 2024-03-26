/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("The content of the article in the specified language")
case class ArticleContentV2(
    @description("The html content") content: String,
    @description("ISO 639-1 code that represents the language used in the content") language: String
)

object ArticleContentV2 {
  implicit val encoder: Encoder[ArticleContentV2] = deriveEncoder
  implicit val decoder: Decoder[ArticleContentV2] = deriveDecoder
}
