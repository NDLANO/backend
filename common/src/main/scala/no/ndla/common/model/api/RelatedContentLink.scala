/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("External link related to the article")
case class RelatedContentLink(
    @description("Title of the article") title: String,
    @description("The url to where the article can be viewed") url: String
)

object RelatedContentLink {
  implicit def encoder: Encoder[RelatedContentLink] = deriveEncoder
  implicit def decoder: Decoder[RelatedContentLink] = deriveDecoder
}
