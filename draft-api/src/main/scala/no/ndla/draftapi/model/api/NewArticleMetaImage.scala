/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

case class NewArticleMetaImage(
    @description("The image-api id of the meta image") id: String,
    @description("The alt text of the meta image") alt: String
)

object NewArticleMetaImage {
  implicit def schema: Schema[NewArticleMetaImage]   = Schema.derived
  implicit def encoder: Encoder[NewArticleMetaImage] = deriveEncoder[NewArticleMetaImage]
  implicit def decoder: Decoder[NewArticleMetaImage] = deriveDecoder[NewArticleMetaImage]
}
