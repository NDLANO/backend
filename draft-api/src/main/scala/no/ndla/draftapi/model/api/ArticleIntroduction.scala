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

@description("Description of the article introduction")
case class ArticleIntroduction(
    @description("The introduction content") introduction: String,
    @description("The html introduction content") htmlIntroduction: String,
    @description(
      "The ISO 639-1 language code describing which article translation this introduction belongs to"
    ) language: String
)

object ArticleIntroduction {
  implicit def encoder: Encoder[ArticleIntroduction] = deriveEncoder
  implicit def decoder: Decoder[ArticleIntroduction] = deriveDecoder
}
