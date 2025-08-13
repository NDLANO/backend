/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the responsible")
case class ResponsibleDTO(
    @description("NDLA ID of responsible editor") responsibleId: String,
    @description("Date of when the responsible editor was last updated") lastUpdated: NDLADate
)

object ResponsibleDTO {
  implicit def encoder: Encoder[ResponsibleDTO]          = deriveEncoder
  implicit def decoder: Decoder[ResponsibleDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[ResponsibleDTO] = sttp.tapir.Schema.derived[ResponsibleDTO]
}
