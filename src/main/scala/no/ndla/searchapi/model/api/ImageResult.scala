/*
 * Part of NDLA search_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field


@ApiModel(description = "Search result for image api")
case class ImageResult(@(ApiModelProperty@field)(description = "The unique id of this image") id: Long,
                       @(ApiModelProperty@field)(description = "A direct link to the image") previewUrl: String,
                       @(ApiModelProperty@field)(description = "A link to get meta data related to the image") metaUrl: String
                      )
