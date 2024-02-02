/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

import no.ndla.network.tapir.NoNullJsonPrinter._

// format: off
@description("Arena topic data")
case class NewTopic(
    @description("The topics title") title: String,
    @description("The initial post in the topic") initialPost: NewPost,
    @description("Whether the topic should be locked or not, only usable by administrators") isLocked: Boolean = false,
    @description("Whether the topic should be pinned to the top of the category, only usable by administrators") isPinned: Boolean = false
)

object NewTopic {
  implicit val encodeMenu: Encoder[NewTopic] = deriveEncoder
  implicit val decodeMenu: Decoder[NewTopic] = deriveConfiguredDecoder
}
