/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Short summary of information about the article")
case class ArticleSummaryV2(
    @(ApiModelProperty @field)(description = "The unique id of the article") id: Long,
    @(ApiModelProperty @field)(description = "The title of the article") title: ArticleTitle,
    @(ApiModelProperty @field)(description = "A visual element article") visualElement: Option[VisualElement],
    @(ApiModelProperty @field)(description = "An introduction for the article") introduction: Option[ArticleIntroduction],
    @(ApiModelProperty @field)(description = "A metaDescription for the article") metaDescription: Option[ArticleMetaDescription],
    @(ApiModelProperty @field)(description = "A meta image for the article") metaImage: Option[ArticleMetaImage],
    @(ApiModelProperty @field)(description = "The full url to where the complete information about the article can be found") url: String,
    @(ApiModelProperty @field)(description = "Describes the license of the article") license: String,
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
    @(ApiModelProperty @field)(description = "The time when the article was last updated") lastUpdated: Date,
    @(ApiModelProperty @field)(description = "A list of available languages for this article") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "A list of codes from GREP API attached to this article") grepCodes: Seq[String],
    @(ApiModelProperty @field)(description = "Value that dictates who gets to see the article. Possible values are: everyone/student/teacher") availability: String,
)
// format: on
