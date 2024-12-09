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

@description("Meta description of the article")
case class ArticleMetaDescriptionDTO(
    @description("The meta description") metaDescription: String,
    @description(
      "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
)

object ArticleMetaDescriptionDTO {
  implicit def encoder: Encoder[ArticleMetaDescriptionDTO] = deriveEncoder
  implicit def decoder: Decoder[ArticleMetaDescriptionDTO] = deriveDecoder
}
