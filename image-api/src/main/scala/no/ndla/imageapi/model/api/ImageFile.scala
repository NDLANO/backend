/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import no.ndla.language.Language.LanguageDocString
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a image file")
case class ImageFile(
    @(ApiModelProperty @field)(description = "File name pointing to image file") fileName: String,
    @(ApiModelProperty @field)(description = "The size of the image in bytes") size: Long,
    @(ApiModelProperty @field)(description = "The mimetype of the image") contentType: String,
    @(ApiModelProperty @field)(description = "The full url to where the image can be downloaded") imageUrl: String,
    @(ApiModelProperty @field)(description = "Dimensions of the image") dimensions: Option[ImageDimensions],
    @(ApiModelProperty @field)(description = LanguageDocString) language: String
)
