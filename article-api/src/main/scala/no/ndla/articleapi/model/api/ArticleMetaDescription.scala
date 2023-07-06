/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta description of the article")
case class ArticleMetaDescription(
    @(ApiModelProperty @field)(description = "The meta description") metaDescription: String,
    @(ApiModelProperty @field)(
      description = "The ISO 639-1 language code describing which article translation this meta description belongs to"
    ) language: String
)

object ArticleMetaDescription {
  implicit val encoder: Encoder.AsObject[ArticleMetaDescription] = deriveEncoder[ArticleMetaDescription]
  implicit val decoder: Decoder[ArticleMetaDescription]          = deriveDecoder[ArticleMetaDescription]
}
