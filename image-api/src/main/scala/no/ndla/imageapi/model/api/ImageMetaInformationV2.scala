/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.Copyright
import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information for the image")
case class ImageMetaInformationV2(
    @description("The unique id of the image") id: String,
    @description("The url to where this information can be found") metaUrl: String,
    @description("The title for the image") title: ImageTitle,
    @description("Alternative text for the image") alttext: ImageAltText,
    @description("The full url to where the image can be downloaded") imageUrl: String,
    @description("The size of the image in bytes") size: Long,
    @description("The mimetype of the image") contentType: String,
    @description("Describes the copyright information for the image") copyright: Copyright,
    @description("Searchable tags for the image") tags: ImageTag,
    @description("Searchable caption for the image") caption: ImageCaption,
    @description("Supported languages for the image title, alt-text, tags and caption.") supportedLanguages: Seq[String],
    @description("Describes when the image was created") created: NDLADate,
    @description("Describes who created the image") createdBy: String,
    @description("Describes if the model has released use of the image") modelRelease: String,
    @description("Describes the changes made to the image, only visible to editors") editorNotes: Option[Seq[EditorNote]],
    @description("Dimensions of the image") imageDimensions: Option[ImageDimensions]
)

object ImageMetaInformationV2 {
  implicit val encoder: Encoder[ImageMetaInformationV2] = deriveEncoder[ImageMetaInformationV2]
  implicit val decoder: Decoder[ImageMetaInformationV2] = deriveDecoder[ImageMetaInformationV2]
}
