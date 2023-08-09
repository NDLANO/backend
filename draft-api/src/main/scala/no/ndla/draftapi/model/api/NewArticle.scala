/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */
// format: off

package no.ndla.draftapi.model.api

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.RelatedContent
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the article")
case class NewArticle(
    @(ApiModelProperty @field)(description = "The chosen language") language: String,
    @(ApiModelProperty @field)(description = "The title of the article") title: String,
    @(ApiModelProperty @field)(description = "The date the article is published") published: Option[NDLADate],
    @(ApiModelProperty @field)(description = "The content of the article") content: Option[String],
    @(ApiModelProperty @field)(description = "Searchable tags") tags: Seq[String],
    @(ApiModelProperty @field)(description = "An introduction") introduction: Option[String],
    @(ApiModelProperty @field)(description = "A meta description") metaDescription: Option[String],
    @(ApiModelProperty @field)(description = "Meta image for the article") metaImage: Option[NewArticleMetaImage],
    @(ApiModelProperty @field)(description = "A visual element for the article. May be anything from an image to a video or H5P") visualElement: Option[String],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the article") copyright: Option[Copyright],
    @(ApiModelProperty @field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @(ApiModelProperty @field)(description = "The notes for this article draft") notes: Seq[String],
    @(ApiModelProperty @field)(description = "The labels attached to this article; meant for editors.") editorLabels: Seq[String],
    @(ApiModelProperty @field)(description = "A list of codes from GREP API connected to the article") grepCodes: Seq[String],
    @(ApiModelProperty @field)(description = "A list of conceptIds connected to the article") conceptIds: Seq[Long],
    @(ApiModelProperty @field)(description = "Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: Option[String],
    @(ApiModelProperty @field)(description = "A list of content related to the article") relatedContent: Seq[RelatedContent],
    @(ApiModelProperty @field)(description = "An object describing a future revision") revisionMeta: Option[Seq[RevisionMeta]],
    @(ApiModelProperty @field)(description = "NDLA ID representing the editor responsible for this article") responsibleId: Option[String],
    @(ApiModelProperty @field)(description = "The path to the frontpage article") slug: Option[String],
    @(ApiModelProperty @field)(description = "Information about a comment attached to an article") comments: List[NewComment],
    @(ApiModelProperty @field)(description = "If the article should be prioritized") prioritized: Option[Boolean]
)
