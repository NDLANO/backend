/*
 * Part of NDLA concept-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the editorial notes")
case class EditorNoteDTO(
    @description("Editorial note") note: String,
    @description("User which saved the note") updatedBy: String,
    @description("Status of concept at saved time") status: StatusDTO,
    @description("Timestamp of when note was saved") timestamp: NDLADate
)

object EditorNoteDTO {
  implicit val encoder: Encoder[EditorNoteDTO] = deriveEncoder
  implicit val decoder: Decoder[EditorNoteDTO] = deriveDecoder
}
