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
import no.ndla.imageapi.model.api.ImageMetaInformationV3DTO

case class BulkUploadItemDTO(
    fileName: Option[String],
    status: BulkUploadItemStatus,
    image: Option[ImageMetaInformationV3DTO],
    error: Option[String],
) {
  def setDone(image: ImageMetaInformationV3DTO): BulkUploadItemDTO =
    this.copy(status = BulkUploadItemStatus.Done, image = Some(image))
  def setUploading(): BulkUploadItemDTO           = this.copy(status = BulkUploadItemStatus.Uploading)
  def setFailed(ex: Throwable): BulkUploadItemDTO =
    this.copy(status = BulkUploadItemStatus.Failed, error = Some(ex.getMessage))
}

object BulkUploadItemDTO {
  implicit val encoder: Encoder[BulkUploadItemDTO] = deriveEncoder
  implicit val decoder: Decoder[BulkUploadItemDTO] = deriveDecoder
}
