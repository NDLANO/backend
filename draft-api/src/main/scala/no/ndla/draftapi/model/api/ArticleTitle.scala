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

@description("Description of a title")
case class ArticleTitle(
    @description("The freetext title of the article") title: String,
    @description("The freetext html title of the article") htmlTitle: String,
    @description("ISO 639-1 code that represents the language used in title") language: String
)

object ArticleTitle {
  implicit def encoder: Encoder[ArticleTitle] = deriveEncoder
  implicit def decoder: Decoder[ArticleTitle] = deriveDecoder
}
