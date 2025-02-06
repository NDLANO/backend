/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the editorial notes")
case class EditorNoteDTO(
    @description("Editorial note") note: String,
    @description("User which saved the note") user: String,
    @description("Status of article at saved time") status: StatusDTO,
    @description("Timestamp of when note was saved") timestamp: NDLADate
)

object EditorNoteDTO {
  implicit def encoder: Encoder[EditorNoteDTO] = deriveEncoder
  implicit def decoder: Decoder[EditorNoteDTO] = deriveDecoder
}
