/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

case class NewArticleMetaImageDTO(
    @description("The image-api id of the meta image") id: String,
    @description("The alt text of the meta image") alt: String
)

object NewArticleMetaImageDTO {
  implicit def schema: Schema[NewArticleMetaImageDTO]   = Schema.derived
  implicit def encoder: Encoder[NewArticleMetaImageDTO] = deriveEncoder[NewArticleMetaImageDTO]
  implicit def decoder: Decoder[NewArticleMetaImageDTO] = deriveDecoder[NewArticleMetaImageDTO]
}
