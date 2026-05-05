/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api.bulk

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

import java.util.UUID

@description("Identifier returned when a bulk upload session has been started")
case class BulkUploadStartedDTO(
    @description("Identifier used to track the bulk upload via the status endpoint")
    uploadId: UUID
)

object BulkUploadStartedDTO {
  implicit val encoder: Encoder[BulkUploadStartedDTO] = deriveEncoder
  implicit val decoder: Decoder[BulkUploadStartedDTO] = deriveDecoder
}
