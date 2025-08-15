/*
 * Part of NDLA draft-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information containing new notes and which draft to add them to")
case class AddNoteDTO(
    @description("Id of the draft to add notes to")
    draftId: Long,
    @description("Notes to add to the draft")
    notes: List[String]
)

object AddNoteDTO {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}
  import sttp.tapir.generic.auto.*

  implicit val encoder: Encoder[AddNoteDTO]          = deriveEncoder
  implicit val decoder: Decoder[AddNoteDTO]          = deriveDecoder
  implicit def schema: sttp.tapir.Schema[AddNoteDTO] = DeriveHelpers.getSchema
}
