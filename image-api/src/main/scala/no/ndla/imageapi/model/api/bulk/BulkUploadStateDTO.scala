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
import no.ndla.common.DeriveHelpers
import no.ndla.imageapi.model.api.ImageMetaInformationV3DTO
import sttp.tapir.Schema

case class BulkUploadStateDTO(
    status: BulkUploadStatus,
    total: Int,
    completed: Int,
    failed: Int,
    items: List[BulkUploadItemDTO],
    error: Option[String],
) {
  private def updateItem(idx: Int, f: BulkUploadItemDTO => BulkUploadItemDTO): BulkUploadStateDTO = {
    val item     = items(idx)
    val newItem  = f(item)
    val newItems = items.updated(idx, newItem)
    this.copy(items = newItems)
  }
  private def incrementCompleted: BulkUploadStateDTO = this.copy(completed = completed + 1)
  private def incrementFailed: BulkUploadStateDTO    = this.copy(failed = failed + 1)

  def asComplete: BulkUploadStateDTO              = this.copy(status = BulkUploadStatus.Complete)
  def asFailed(error: String): BulkUploadStateDTO = this.copy(status = BulkUploadStatus.Failed, error = Some(error))
  def asFailed(ex: Throwable): BulkUploadStateDTO = asFailed(ex.getMessage)

  def setDone(idx: Int, image: ImageMetaInformationV3DTO): BulkUploadStateDTO = {
    val withUpdatedItem = this.updateItem(idx, _.setDone(image))
    withUpdatedItem.incrementCompleted
  }

  def setFailed(idx: Int, ex: Throwable): BulkUploadStateDTO = {
    val withUpdatedItem = this.updateItem(idx, _.setFailed(ex))
    withUpdatedItem.incrementFailed
  }

  def setUploading(idx: Int): BulkUploadStateDTO = updateItem(idx, _.setUploading())
}

object BulkUploadStateDTO {
  implicit val encoder: Encoder[BulkUploadStateDTO] = deriveEncoder
  implicit val decoder: Decoder[BulkUploadStateDTO] = deriveDecoder
  import sttp.tapir.generic.auto.*
  implicit val schema: Schema[BulkUploadStateDTO] = DeriveHelpers.getSchema
}
