/*
 * Part of NDLA article-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Meta description of the article")
case class ArticleMetaImage(
    @description("The meta image url") url: String,
    @description("The alt text for the meta image") alt: String,
    @description(
      "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
)

object ArticleMetaImage {
  implicit val encoder: Encoder[ArticleMetaImage] = deriveEncoder
  implicit val decoder: Decoder[ArticleMetaImage] = deriveDecoder
}
