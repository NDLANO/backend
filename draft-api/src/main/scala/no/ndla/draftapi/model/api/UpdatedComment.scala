/*
 * Part of NDLA draft-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about a comment attached to an article")
case class UpdatedComment(
    @(ApiModelProperty @field)(description = "Id of the comment") id: Option[String],
    @(ApiModelProperty @field)(description = "Content of the comment") content: String,
    @(ApiModelProperty @field)(description = "If the comment is open or closed") isOpen: Option[Boolean],
    @(ApiModelProperty @field)(description = "If the comment is solved or not") solved: Option[Boolean]
)

object UpdatedComment {
  implicit def encoder: Encoder[UpdatedComment] = deriveEncoder
  implicit def decoder: Decoder[UpdatedComment] = deriveDecoder
}
