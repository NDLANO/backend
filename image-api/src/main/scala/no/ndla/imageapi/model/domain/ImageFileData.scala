/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.WithLanguage

case class ImageFileData(
    fileName: String,
    size: Long,
    contentType: String,
    dimensions: Option[ImageDimensions],
    variants: Seq[ImageVariant],
    override val language: String,
) extends WithLanguage {
  def getFileStem: String = {
    fileName.lastIndexOf(".") match {
      case i if i > 0 => fileName.substring(0, i)
      case _          => fileName
    }
  }
}

object ImageFileData {
  implicit val encoder: Encoder[ImageFileData] = deriveEncoder
  implicit val decoder: Decoder[ImageFileData] = deriveDecoder
}
