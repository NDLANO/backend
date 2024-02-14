/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "External link related to the article")
case class RelatedContentLink(
    @(ApiModelProperty @field)(description = "Title of the article") title: String,
    @(ApiModelProperty @field)(description = "The url to where the article can be viewed") url: String
)

object RelatedContentLink {
  implicit def encoder: Encoder[RelatedContentLink] = deriveEncoder
  implicit def decoder: Decoder[RelatedContentLink] = deriveDecoder
}
