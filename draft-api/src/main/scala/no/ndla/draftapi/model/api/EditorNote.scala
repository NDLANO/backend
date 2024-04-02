/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Information about the editorial notes")
case class EditorNote(
    @description("Editorial note") note: String,
    @description("User which saved the note") user: String,
    @description("Status of article at saved time") status: Status,
    @description("Timestamp of when note was saved") timestamp: NDLADate
)

object EditorNote {
  implicit def encoder: Encoder[EditorNote] = deriveEncoder
  implicit def decoder: Decoder[EditorNote] = deriveDecoder
}
