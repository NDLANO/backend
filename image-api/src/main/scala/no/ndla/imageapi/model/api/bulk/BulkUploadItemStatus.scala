/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api.bulk

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

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
