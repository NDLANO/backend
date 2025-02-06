/*
 * Part of NDLA search-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SearchableStatus(
    current: String,
    other: Seq[String]
)

object SearchableStatus {
  implicit val encoder: Encoder[SearchableStatus] = deriveEncoder
  implicit val decoder: Decoder[SearchableStatus] = deriveDecoder
}
