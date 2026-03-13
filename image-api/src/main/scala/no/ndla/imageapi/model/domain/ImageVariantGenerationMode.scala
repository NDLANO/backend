/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import enumeratum.{Enum, EnumEntry}

sealed trait ImageVariantGenerationMode extends EnumEntry

object ImageVariantGenerationMode extends Enum[ImageVariantGenerationMode] {
  case object MissingOnly extends ImageVariantGenerationMode
  case object ReplaceAll  extends ImageVariantGenerationMode

  val values: IndexedSeq[ImageVariantGenerationMode] = findValues
}
