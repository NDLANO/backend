/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.{Enum, EnumEntry}
import no.ndla.common.CirceUtil.CirceEnumWithErrors
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

sealed abstract class ImageContentType(override val entryName: String, val fileEndings: List[String])
    extends EnumEntry {
  override def toString: String = entryName
}

object ImageContentType extends Enum[ImageContentType], CirceEnumWithErrors[ImageContentType] {
  case object Bmp             extends ImageContentType("image/bmp", List(".bmp"))
  case object Gif             extends ImageContentType("image/gif", List(".gif"))
  case object Jpeg            extends ImageContentType("image/jpeg", List(".jpg", ".jpeg"))
  case object JpegCitrix      extends ImageContentType("image/x-citrix-jpeg", List(".jpg", ".jpeg"))
  case object JpegProgressive extends ImageContentType("image/pjpeg", List(".jpg", ".jpeg"))
  case object Png             extends ImageContentType("image/png", List(".png"))
  case object PngXToken       extends ImageContentType("image/x-png", List(".png"))
  case object Svg             extends ImageContentType("image/svg+xml", List(".svg"))
  case object Webp            extends ImageContentType("image/webp", List(".webp"))

  override def values: IndexedSeq[ImageContentType] = findValues
  implicit def schema: Schema[ImageContentType]     = schemaForEnumEntry[ImageContentType]
}
