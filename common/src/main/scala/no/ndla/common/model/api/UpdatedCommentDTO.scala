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
import sttp.tapir.Schema.annotations.description

@description("Information about a comment attached to an article")
case class UpdatedCommentDTO(
    @description("Id of the comment") id: Option[String],
    @description("Content of the comment") content: String,
    @description("If the comment is open or closed") isOpen: Option[Boolean],
    @description("If the comment is solved or not") solved: Option[Boolean]
)

object UpdatedCommentDTO {
  implicit def encoder: Encoder[UpdatedCommentDTO] = deriveEncoder
  implicit def decoder: Decoder[UpdatedCommentDTO] = deriveDecoder
}
