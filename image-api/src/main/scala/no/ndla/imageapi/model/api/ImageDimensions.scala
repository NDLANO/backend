/*
 * Part of NDLA image-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Dimensions of an image")
case class ImageDimensions(
    @(ApiModelProperty @field)(description = "The width of the image in pixels") width: Int,
    @(ApiModelProperty @field)(description = "The height of the image in pixels") height: Int
)
