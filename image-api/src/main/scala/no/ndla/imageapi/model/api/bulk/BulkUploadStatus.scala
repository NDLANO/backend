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
