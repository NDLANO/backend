/*
 * Part of NDLA search-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SearchableGrepContext(
    code: String,
    title: Option[String]
)

object SearchableGrepContext {
  implicit val encoder: Encoder[SearchableGrepContext] = deriveEncoder
  implicit val decoder: Decoder[SearchableGrepContext] = deriveDecoder
}
