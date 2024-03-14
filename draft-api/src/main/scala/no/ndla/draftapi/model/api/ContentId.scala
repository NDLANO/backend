/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Id for a single Article")
case class ContentId(@description("The unique id of the article") id: Long)

object ContentId {
  implicit val encoder: Encoder[ContentId] = deriveEncoder
  implicit val decoder: Decoder[ContentId] = deriveDecoder
}
