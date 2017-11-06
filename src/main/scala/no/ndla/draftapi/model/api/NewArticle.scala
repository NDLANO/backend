/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field
@ApiModel(description = "Information about the article")
case class NewArticle(@(ApiModelProperty@field)(description = "The chosen language") language: String,
                      @(ApiModelProperty@field)(description = "The title of the article") title: String,
                      @(ApiModelProperty@field)(description = "The content of the article") content: Option[String],
                      @(ApiModelProperty@field)(description = "Searchable tags") tags: Seq[String],
                      @(ApiModelProperty@field)(description = "An introduction") introduction: Option[String],
                      @(ApiModelProperty@field)(description = "A meta description") metaDescription: Option[String],
                      @(ApiModelProperty@field)(description = "An image-api ID for the article meta image") metaImageId: Option[String],
                      @(ApiModelProperty@field)(description = "A visual element for the article. May be anything from an image to a video or H5P") visualElement: Option[String],
                      @(ApiModelProperty@field)(description = "Describes the copyright information for the article") copyright: Option[Copyright],
                      @(ApiModelProperty@field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
                      @(ApiModelProperty@field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: Option[String]
                     )
