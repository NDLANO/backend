/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty
import sttp.tapir.Schema

import scala.annotation.meta.field

case class NewArticleMetaImage(
    @(ApiModelProperty @field)(description = "The image-api id of the meta image") id: String,
    @(ApiModelProperty @field)(description = "The alt text of the meta image") alt: String
)

object NewArticleMetaImage {
  implicit def schema: Schema[NewArticleMetaImage]   = Schema.derived
  implicit def encoder: Encoder[NewArticleMetaImage] = deriveEncoder[NewArticleMetaImage]
  implicit def decoder: Decoder[NewArticleMetaImage] = deriveDecoder[NewArticleMetaImage]
}
