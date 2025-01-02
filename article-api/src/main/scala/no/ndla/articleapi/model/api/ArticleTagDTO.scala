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

@description("Description of the tags of the article")
case class ArticleTagDTO(
    @description("The searchable tag.") tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag") language: String
)

object ArticleTagDTO {
  implicit val encoder: Encoder.AsObject[ArticleTagDTO] = deriveEncoder[ArticleTagDTO]
  implicit val decoder: Decoder[ArticleTagDTO]          = deriveDecoder[ArticleTagDTO]
}
