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

enum BulkUploadStatus {
  case Pending,
    Running,
    Complete,
    Failed
}

object BulkUploadStatus {
  implicit val schema: Schema[BulkUploadStatus]   = Schema.derivedEnumeration.defaultStringBased
  implicit val encoder: Encoder[BulkUploadStatus] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[BulkUploadStatus] = Decoder
    .decodeString
    .emap { s =>
      BulkUploadStatus.values.find(_.toString == s).toRight(s"Unknown BulkUploadStatus: $s")
    }
}

enum BulkUploadItemStatus {
  case Pending,
    Uploading,
    Done,
    Failed
}

object BulkUploadItemStatus {
  implicit val schema: Schema[BulkUploadItemStatus]   = Schema.derivedEnumeration.defaultStringBased
  implicit val encoder: Encoder[BulkUploadItemStatus] = Encoder.encodeString.contramap(_.toString)
  implicit val decoder: Decoder[BulkUploadItemStatus] = Decoder
    .decodeString
    .emap { s =>
      BulkUploadItemStatus.values.find(_.toString == s).toRight(s"Unknown BulkUploadItemStatus: $s")
    }
}

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
