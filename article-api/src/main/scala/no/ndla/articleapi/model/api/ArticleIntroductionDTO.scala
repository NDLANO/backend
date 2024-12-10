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

@description("Description of the article introduction")
case class ArticleIntroductionDTO(
    @description("The introduction content") introduction: String,
    @description("The html-version introduction content") htmlIntroduction: String,
    @description(
      "The ISO 639-1 language code describing which article translation this introduction belongs to"
    ) language: String
)

object ArticleIntroductionDTO {
  implicit val encoder: Encoder[ArticleIntroductionDTO] = deriveEncoder
  implicit val decoder: Decoder[ArticleIntroductionDTO] = deriveDecoder
}
