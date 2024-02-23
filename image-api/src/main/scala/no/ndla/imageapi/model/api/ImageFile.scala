/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.Language.LanguageDocString
import sttp.tapir.Schema.annotations.description

@description("Meta information for a image file")
case class ImageFile(
    @description("File name pointing to image file") fileName: String,
    @description("The size of the image in bytes") size: Long,
    @description("The mimetype of the image") contentType: String,
    @description("The full url to where the image can be downloaded") imageUrl: String,
    @description("Dimensions of the image") dimensions: Option[ImageDimensions],
    @description(LanguageDocString) language: String
)

object ImageFile {
  implicit val encoder: Encoder[ImageFile] = deriveEncoder
  implicit val decoder: Decoder[ImageFile] = deriveDecoder
}
