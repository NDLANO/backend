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
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.CopyrightDTO
import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information for the image")
case class ImageMetaInformationV3DTO(
                                      @description("The unique id of the image") id: String,
                                      @description("The url to where this information can be found") metaUrl: String,
                                      @description("The title for the image") title: ImageTitleDTO,
                                      @description("Alternative text for the image") alttext: ImageAltTextDTO,
                                      @description("Describes the copyright information for the image") copyright: CopyrightDTO,
                                      @description("Searchable tags for the image") tags: ImageTagDTO,
                                      @description("Searchable caption for the image") caption: ImageCaptionDTO,
                                      @description("Supported languages for the image title, alt-text, tags and caption.") supportedLanguages: Seq[String],
                                      @description("Describes when the image was created") created: NDLADate,
                                      @description("Describes who created the image") createdBy: String,
                                      @description("Describes if the model has released use of the image") modelRelease: String,
                                      @description("Describes the changes made to the image, only visible to editors") editorNotes: Option[Seq[EditorNoteDTO]],
                                      @description("Describes the image file") image: ImageFileDTO
)

object ImageMetaInformationV3DTO{
  implicit val encoder: Encoder[ImageMetaInformationV3DTO] = deriveEncoder
  implicit val decoder: Decoder[ImageMetaInformationV3DTO] = deriveDecoder
}
