/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a new learningpath")
case class UpdatedLearningPathV2(@(ApiModelProperty@field)(description = "The revision number for this learningpath") revision: Int,
                                 @(ApiModelProperty@field)(description = "The title of the learningpath") title: Option[String],
                                 @(ApiModelProperty@field)(description = "The chosen language") language: String,
                                 @(ApiModelProperty@field)(description = "The description of the learningpath") description: Option[String],
                                 @(ApiModelProperty@field)(description = "Url to cover-photo in NDLA image-api.") coverPhotoMetaUrl: Option[String],
                                 @(ApiModelProperty@field)(description = "The duration of the learningpath in minutes. Must be greater than 0") duration: Option[Int],
                                 @(ApiModelProperty@field)(description = "Searchable tags for the learningpath") tags: Option[Seq[String]],
                                 @(ApiModelProperty@field)(description = "Describes the copyright information for the learningpath") copyright: Option[Copyright])
