/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SearchableTag(
    tag: String,
    language: String
)

object SearchableTag {
  implicit val encoder: Encoder[SearchableTag] = deriveEncoder
  implicit val decoder: Decoder[SearchableTag] = deriveDecoder
}
