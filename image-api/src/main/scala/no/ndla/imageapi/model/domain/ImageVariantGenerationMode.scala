/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.{Enum, EnumEntry}

sealed abstract class ImageVariantGenerationMode(override val entryName: String) extends EnumEntry

object ImageVariantGenerationMode extends Enum[ImageVariantGenerationMode] {
  case object MissingOnly extends ImageVariantGenerationMode("missing_only")
  case object ReplaceAll  extends ImageVariantGenerationMode("replace_all")

  override def values: IndexedSeq[ImageVariantGenerationMode] = findValues
}
