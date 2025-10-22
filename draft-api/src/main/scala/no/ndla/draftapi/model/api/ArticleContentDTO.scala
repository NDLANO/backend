/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema
import no.ndla.common.DeriveHelpers

@description("The content of the article in the specified language")
case class ArticleContentDTO(
    @description("The html content")
    content: String,
    @description("ISO 639-1 code that represents the language used in the content")
    language: String,
)

object ArticleContentDTO {
  implicit def encoder: Encoder[ArticleContentDTO] = deriveEncoder
  implicit def decoder: Decoder[ArticleContentDTO] = deriveDecoder
  implicit def schema: Schema[ArticleContentDTO]   = DeriveHelpers.getSchema[ArticleContentDTO]
}
