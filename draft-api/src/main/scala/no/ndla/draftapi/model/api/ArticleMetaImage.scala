/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta description of the article")
case class ArticleMetaImage(
    @(ApiModelProperty @field)(description = "The meta image") url: String,
    @(ApiModelProperty @field)(description = "The meta image alt text") alt: String,
    @(ApiModelProperty @field)(
      description = "The ISO 639-1 language code describing which article translation this meta image belongs to"
    ) language: String
)

object ArticleMetaImage {
  implicit def encoder: Encoder[ArticleMetaImage] = deriveEncoder
  implicit def decoder: Decoder[ArticleMetaImage] = deriveDecoder
}
