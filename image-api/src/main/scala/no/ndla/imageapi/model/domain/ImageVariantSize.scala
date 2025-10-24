/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.{Enum, EnumEntry}
import no.ndla.common.CirceUtil.CirceEnumWithErrors

sealed abstract class ImageVariantSize(override val entryName: String, val width: Int) extends EnumEntry

object ImageVariantSize extends Enum[ImageVariantSize], CirceEnumWithErrors[ImageVariantSize] {
  case object Icon   extends ImageVariantSize("icon", 250)
  case object Small  extends ImageVariantSize("small", 500)
  case object Medium extends ImageVariantSize("medium", 1000)
  case object Large  extends ImageVariantSize("large", 2000)

  override def values: IndexedSeq[ImageVariantSize] = findValues

  def forDimensions(dimensions: ImageDimensions): Seq[ImageVariantSize] = values
    .sortBy(_.width)
    .takeWhile(_.width <= dimensions.width)
}
