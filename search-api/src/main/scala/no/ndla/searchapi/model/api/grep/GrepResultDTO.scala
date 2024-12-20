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
import no.ndla.searchapi.model.api.TitleDTO
import sttp.tapir.Schema.annotations.description

@description("Information about a single grep search result entry")
case class GrepResultDTO(
    @description("The grep code") code: String,
    @description("The greps title") title: TitleDTO,
    @description("The grep laereplan") laereplanCode: Option[String]
)

object GrepResultDTO {
  implicit val encoder: Encoder[GrepResultDTO] = deriveEncoder
  implicit val decoder: Decoder[GrepResultDTO] = deriveDecoder
}
