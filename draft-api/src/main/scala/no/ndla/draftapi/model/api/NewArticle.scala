/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */
// format: off

package no.ndla.draftapi.model.api

import java.util.Date
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Information about the article")
case class NewArticle(
    @(ApiModelProperty @field)(description = "The chosen language") language: String,
    @(ApiModelProperty @field)(description = "The title of the article") title: String,
    @(ApiModelProperty @field)(description = "The date the article is published") published: Option[Date],
    @(ApiModelProperty @field)(description = "The content of the article") content: Option[String],
    @(ApiModelProperty @field)(description = "Searchable tags") tags: Seq[String],
    @(ApiModelProperty @field)(description = "An introduction") introduction: Option[String],
    @(ApiModelProperty @field)(description = "A meta description") metaDescription: Option[String],
    @(ApiModelProperty @field)(description = "Meta image for the article") metaImage: Option[NewArticleMetaImage],
    @(ApiModelProperty @field)(description = "A visual element for the article. May be anything from an image to a video or H5P") visualElement: Option[String],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the article") copyright: Option[Copyright],
    @(ApiModelProperty @field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
    @(ApiModelProperty @field)(description = "The notes for this article draft") notes: Seq[String],
    @(ApiModelProperty @field)(description = "The labels attached to this article; meant for editors.") editorLabels: Seq[String],
    @(ApiModelProperty @field)(description = "A list of codes from GREP API connected to the article") grepCodes: Seq[String],
    @(ApiModelProperty @field)(description = "A list of conceptIds connected to the article") conceptIds: Seq[Long],
    @(ApiModelProperty @field)(description = "Value that dictates who gets to see the article. Possible values are: everyone/student/teacher") availability: Option[String],
    @(ApiModelProperty @field)(description = "A list of content related to the article") relatedContent: Seq[RelatedContent]
)
