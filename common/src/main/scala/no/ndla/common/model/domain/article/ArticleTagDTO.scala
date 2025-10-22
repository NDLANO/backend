/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.article

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Description of the tags of the article")
case class ArticleTagDTO(
    @description("The searchable tag.")
    tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag")
    language: String,
)

object ArticleTagDTO {
  implicit val encoder: Encoder.AsObject[ArticleTagDTO] = deriveEncoder[ArticleTagDTO]
  implicit val decoder: Decoder[ArticleTagDTO]          = deriveDecoder[ArticleTagDTO]
}
