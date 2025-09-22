/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.DeriveHelpers
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

@description("Information about a comment attached to an article")
case class NewCommentDTO(
    @description("Content of the comment") content: String,
    @description("If the comment is open or closed") isOpen: Option[Boolean]
)

object NewCommentDTO {
  implicit def encoder: Encoder[NewCommentDTO] = deriveEncoder
  implicit def decoder: Decoder[NewCommentDTO] = deriveDecoder
  implicit def schema: Schema[NewCommentDTO]   = DeriveHelpers.getSchema
}
