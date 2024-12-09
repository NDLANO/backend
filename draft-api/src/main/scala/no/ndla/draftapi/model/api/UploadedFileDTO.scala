/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Information about the uploaded file")
case class UploadedFileDTO(
    @description("Uploaded file's basename") filename: String,
    @description("Uploaded file's mime type") mime: String,
    @description("Uploaded file's file extension") extension: String,
    @description("Full path of uploaded file") path: String
)

object UploadedFileDTO {
  implicit val encoder: Encoder[UploadedFileDTO] = deriveEncoder
  implicit val decoder: Decoder[UploadedFileDTO] = deriveDecoder
}
