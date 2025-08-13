/*
 * Part of NDLA common
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

@description("External link related to the article")
case class RelatedContentLinkDTO(
    @description("Title of the article") title: String,
    @description("The url to where the article can be viewed") url: String
)

object RelatedContentLinkDTO {
  implicit def encoder: Encoder[RelatedContentLinkDTO] = deriveEncoder
  implicit def decoder: Decoder[RelatedContentLinkDTO] = deriveDecoder
  implicit def schema: Schema[RelatedContentLinkDTO]   = Schema.derived
}
