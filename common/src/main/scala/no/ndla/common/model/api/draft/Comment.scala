/*
 * Part of NDLA common.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api.draft

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about a comment attached to an article")
case class Comment(
    @description("Id of the comment") id: String,
    @description("Content of the comment") content: String,
    @description("When the comment was created") created: NDLADate,
    @description("When the comment was last updated") updated: NDLADate,
    @description("If the comment is open or closed") isOpen: Boolean,
    @description("If the comment is solved or not") solved: Boolean
)

object Comment {
  implicit def encoder: Encoder[Comment] = deriveEncoder
  implicit def decoder: Decoder[Comment] = deriveDecoder
}
