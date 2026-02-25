/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.{Enum, EnumEntry}

sealed trait ProcessableImageFormat extends EnumEntry {
  def toContentType: ImageContentType = this match {
    case ProcessableImageFormat.Jpeg => ImageContentType.Jpeg
    case ProcessableImageFormat.Png  => ImageContentType.Png
    case ProcessableImageFormat.Webp => ImageContentType.Webp
  }
}

object ProcessableImageFormat extends Enum[ProcessableImageFormat] {
  case object Jpeg extends ProcessableImageFormat
  case object Png  extends ProcessableImageFormat
  case object Webp extends ProcessableImageFormat

  override def values: IndexedSeq[ProcessableImageFormat] = findValues
}
