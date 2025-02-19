/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.arena.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Arena post data")
case class NewPostDTO(
    @description("The post content") content: String,
    @description("The id of the post that is replied to") toPostId: Option[Long]
)

object NewPostDTO {
  implicit val encodeMenu: Encoder[NewPostDTO] = deriveEncoder
  implicit val decodeMenu: Decoder[NewPostDTO] = deriveDecoder
}
