/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.grep

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.searchapi.model.api.Title
import sttp.tapir.Schema.annotations.description

@description("Information about a single grep search result entry")
case class GrepResult(
    @description("The grep code") code: String,
    @description("The greps title") title: Title
)

object GrepResult {
  implicit val encoder: Encoder[GrepResult] = deriveEncoder
  implicit val decoder: Decoder[GrepResult] = deriveDecoder
}
