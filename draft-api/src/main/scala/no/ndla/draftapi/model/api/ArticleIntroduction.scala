/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of the article introduction")
case class ArticleIntroduction(
    @(ApiModelProperty @field)(description = "The introduction content") introduction: String,
    @(ApiModelProperty @field)(description = "The html introduction content") htmlIntroduction: String,
    @(ApiModelProperty @field)(
      description = "The ISO 639-1 language code describing which article translation this introduction belongs to"
    ) language: String
)

object ArticleIntroduction {
  implicit def encoder: Encoder[ArticleIntroduction] = deriveEncoder
  implicit def decoder: Decoder[ArticleIntroduction] = deriveDecoder
}
