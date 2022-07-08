/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import java.time.LocalDateTime
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Summary of meta information for an image")
case class ImageMetaSummary(
    @(ApiModelProperty @field)(description = "The unique id of the image") id: String,
    @(ApiModelProperty @field)(description = "The title for this image") title: ImageTitle,
    @(ApiModelProperty @field)(description = "The copyright authors for this image") contributors: Seq[String],
    @(ApiModelProperty @field)(description = "The alt text for this image") altText: ImageAltText,
    @(ApiModelProperty @field)(description = "The caption for this image") caption: ImageCaption,
    @(ApiModelProperty @field)(description = "The full url to where a preview of the image can be downloaded") previewUrl: String,
    @(ApiModelProperty @field)(description = "The full url to where the complete metainformation about the image can be found") metaUrl: String,
    @(ApiModelProperty @field)(description = "Describes the license of the image") license: String,
    @(ApiModelProperty @field)(description = "List of supported languages in priority") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "Describes if the model has released use of the image", allowableValues = "not-set,yes,no,not-applicable") modelRelease: Option[String],
    @(ApiModelProperty @field)(description = "Describes the changes made to the image, only visible to editors") editorNotes: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "The time and date of last update") lastUpdated: LocalDateTime,
    @(ApiModelProperty @field)(description = "The size of the image in bytes") fileSize: Long,
    @(ApiModelProperty @field)(description = "The mimetype of the image") contentType: String,
    @(ApiModelProperty @field)(description = "Dimensions of the image") imageDimensions: Option[ImageDimensions]
)
