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
import no.ndla.common.DeriveHelpers

@description("Description of the article introduction")
case class ArticleIntroductionDTO(
    @description("The introduction content")
    introduction: String,
    @description("The html introduction content")
    htmlIntroduction: String,
    @description("The ISO 639-1 language code describing which article translation this introduction belongs to")
    language: String
)

object ArticleIntroductionDTO {
  implicit def encoder: Encoder[ArticleIntroductionDTO]          = deriveEncoder
  implicit def decoder: Decoder[ArticleIntroductionDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[ArticleIntroductionDTO] = DeriveHelpers.getSchema[ArticleIntroductionDTO]
}
