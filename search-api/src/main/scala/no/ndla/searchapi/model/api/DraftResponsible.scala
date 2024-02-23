/*
 * Part of NDLA search-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the responsible")
case class DraftResponsible(
    @description("NDLA ID of responsible editor") responsibleId: String,
    @description("Date of when the responsible editor was last updated") lastUpdated: NDLADate
)

object DraftResponsible {
  implicit val encoder: Encoder[DraftResponsible] = deriveEncoder
  implicit val decoder: Decoder[DraftResponsible] = deriveDecoder
}
