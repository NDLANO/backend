/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.model.arena.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.generic.semiauto.deriveEncoder
import sttp.tapir.Schema.annotations.description

import no.ndla.network.tapir.NoNullJsonPrinter._

@description("Arena post data")
case class NewPost(
    @description("The post content") content: String
)

object NewPost {
  implicit val encodeMenu: Encoder[NewPost] = deriveEncoder
  implicit val decodeMenu: Decoder[NewPost] = deriveConfiguredDecoder
}
