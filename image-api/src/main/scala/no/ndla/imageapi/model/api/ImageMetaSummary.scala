/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

// format: off
@description("Summary of meta information for an image")
case class ImageMetaSummary(
    @description("The unique id of the image") id: String,
    @description("The title for this image") title: ImageTitle,
    @description("The copyright authors for this image") contributors: Seq[String],
    @description("The alt text for this image") altText: ImageAltText,
    @description("The caption for this image") caption: ImageCaption,
    @description("The full url to where a preview of the image can be downloaded") previewUrl: String,
    @description("The full url to where the complete metainformation about the image can be found") metaUrl: String,
    @description("Describes the license of the image") license: String,
    @description("List of supported languages in priority") supportedLanguages: Seq[String],
    @description("Describes if the model has released use of the image") modelRelease: Option[String],
    @description("Describes the changes made to the image, only visible to editors") editorNotes: Option[Seq[String]],
    @description("The time and date of last update") lastUpdated: NDLADate,
    @description("The size of the image in bytes") fileSize: Long,
    @description("The mimetype of the image") contentType: String,
    @description("Dimensions of the image") imageDimensions: Option[ImageDimensions]
)

object ImageMetaSummary {
  implicit val encoder: Encoder[ImageMetaSummary] = deriveEncoder
  implicit val decoder: Decoder[ImageMetaSummary] = deriveDecoder
}
