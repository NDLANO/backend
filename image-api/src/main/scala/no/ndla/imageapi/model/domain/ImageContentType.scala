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

sealed abstract class ImageContentType(override val entryName: String, val fileEndings: List[String])
    extends EnumEntry {
  override def toString: String = entryName
}

object ImageContentType extends Enum[ImageContentType], CirceEnumWithErrors[ImageContentType] {
  case object Jpeg            extends ImageContentType("image/jpeg", List(".jpg", ".jpeg"))
  case object JpegCitrix      extends ImageContentType("image/x-citrix-jpeg", List(".jpg", ".jpeg"))
  case object JpegProgressive extends ImageContentType("image/pjpeg", List(".jpg", ".jpeg"))
  case object Png             extends ImageContentType("image/png", List(".png"))
  case object PngXToken       extends ImageContentType("image/x-png", List(".png"))
  case object Gif             extends ImageContentType("image/gif", List(".gif"))
  case object Webp            extends ImageContentType("image/webp", List(".webp"))
  case object Svg             extends ImageContentType("image/svg+xml", List(".svg"))
  case object Bmp             extends ImageContentType("image/bmp", List(".bmp"))

  override def values: IndexedSeq[ImageContentType] = findValues

  def valueOf(s: String): Option[ImageContentType] = ImageContentType.values.find(_.entryName == s)
}
