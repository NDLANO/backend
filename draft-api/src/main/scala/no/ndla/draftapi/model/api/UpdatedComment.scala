/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about a comment attached to an article")
case class UpdatedComment(
    @description("Id of the comment") id: Option[String],
    @description("Content of the comment") content: String,
    @description("If the comment is open or closed") isOpen: Option[Boolean],
    @description("If the comment is solved or not") solved: Option[Boolean]
)

object UpdatedComment {
  implicit def encoder: Encoder[UpdatedComment] = deriveEncoder
  implicit def decoder: Decoder[UpdatedComment] = deriveDecoder
}
