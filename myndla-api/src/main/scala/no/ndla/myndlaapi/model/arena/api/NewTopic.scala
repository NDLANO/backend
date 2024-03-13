/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Arena topic data")
case class NewTopic(
    @description("The topics title") title: String,
    @description("The initial post in the topic") initialPost: NewPost,
    @description("Whether the topic should be locked or not, only usable by administrators") isLocked: Option[Boolean],
    @description("Whether the topic should be pinned to the top of the category, only usable by administrators") isPinned: Option[Boolean]
)

object NewTopic {
  implicit val encodeMenu: Encoder[NewTopic] = deriveEncoder
  implicit val decodeMenu: Decoder[NewTopic] = deriveDecoder
}
