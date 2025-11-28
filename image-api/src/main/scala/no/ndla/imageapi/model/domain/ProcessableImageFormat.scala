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
  def toContentType: String = this match {
    case ProcessableImageFormat.Jpeg => "image/jpeg"
    case ProcessableImageFormat.Png  => "image/png"
    case ProcessableImageFormat.Webp => "image/webp"
  }
}

object ProcessableImageFormat extends Enum[ProcessableImageFormat] {
  case object Jpeg extends ProcessableImageFormat
  case object Png  extends ProcessableImageFormat
  case object Webp extends ProcessableImageFormat

  override def values: IndexedSeq[ProcessableImageFormat] = findValues
}
