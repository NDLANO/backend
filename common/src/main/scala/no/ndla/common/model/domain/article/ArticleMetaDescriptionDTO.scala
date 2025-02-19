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

@description("Meta description of the article")
case class ArticleMetaDescriptionDTO(
    @description("The meta description") metaDescription: String,
    @description(
      "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
)

object ArticleMetaDescriptionDTO {
  implicit val encoder: Encoder.AsObject[ArticleMetaDescriptionDTO] = deriveEncoder[ArticleMetaDescriptionDTO]
  implicit val decoder: Decoder[ArticleMetaDescriptionDTO]          = deriveDecoder[ArticleMetaDescriptionDTO]
}
