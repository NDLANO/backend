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
case class NewComment(
    @description("Content of the comment") content: String,
    @description("If the comment is open or closed") isOpen: Option[Boolean]
)

object NewComment {
  implicit def encoder: Encoder[NewComment] = deriveEncoder
  implicit def decoder: Decoder[NewComment] = deriveDecoder
}
