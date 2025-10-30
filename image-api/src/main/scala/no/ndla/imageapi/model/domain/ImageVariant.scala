/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.CirceUtil.CirceEnumWithErrors
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.*

case class ImageVariant(size: ImageVariantSize, bucketKey: String) {
  def sizeName: String = size.entryName
}

object ImageVariant {
  implicit val encoder: Encoder[ImageVariant] = deriveEncoder[ImageVariant]
  implicit val decoder: Decoder[ImageVariant] = deriveDecoder[ImageVariant]
}

sealed abstract class ImageVariantSize(override val entryName: String, val width: Int) extends EnumEntry

object ImageVariantSize extends Enum[ImageVariantSize], CirceEnumWithErrors[ImageVariantSize] {
  case object Icon       extends ImageVariantSize("icon", 320)
  case object ExtraSmall extends ImageVariantSize("xsmall", 640)
  case object Small      extends ImageVariantSize("small", 960)
  case object Medium     extends ImageVariantSize("medium", 1280)
  case object Large      extends ImageVariantSize("large", 1920)
  case object ExtraLarge extends ImageVariantSize("xlarge", 2400)

  def forDimensions(dimensions: ImageDimensions): Seq[ImageVariantSize] = values.takeWhile(_.width <= dimensions.width)

  override def values: IndexedSeq[ImageVariantSize] = findValues

  implicit def schema: Schema[ImageVariantSize] = schemaForEnumEntry[ImageVariantSize]
}
