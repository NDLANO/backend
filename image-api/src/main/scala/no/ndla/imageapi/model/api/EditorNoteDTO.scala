/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Note about a change that happened to the image")
case class EditorNoteDTO(
    @description("Timestamp of the change") timestamp: NDLADate,
    @description("Who triggered the change") updatedBy: String,
    @description("Editorial note") note: String
)

object EditorNoteDTO {
  implicit val encoder: Encoder[EditorNoteDTO] = deriveEncoder[EditorNoteDTO]
  implicit val decoder: Decoder[EditorNoteDTO] = deriveDecoder[EditorNoteDTO]
}
