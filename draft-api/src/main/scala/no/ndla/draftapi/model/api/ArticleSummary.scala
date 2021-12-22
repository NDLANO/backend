/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Short summary of information about the article")
case class ArticleSummary(
    @(ApiModelProperty @field)(description = "The unique id of the article") id: Long,
    @(ApiModelProperty @field)(description = "The title of the article") title: ArticleTitle,
    @(ApiModelProperty @field)(description = "A visual element article") visualElement: Option[VisualElement],
    @(ApiModelProperty @field)(description = "An introduction for the article") introduction: Option[ArticleIntroduction],
    @(ApiModelProperty @field)(description = "The full url to where the complete information about the article can be found") url: String,
    @(ApiModelProperty @field)(description = "Describes the license of the article") license: String,
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
    @(ApiModelProperty @field)(description = "A list of available languages for this audio") supportedLanguages: Seq[String],
    @(ApiModelProperty @field)(description = "Searchable tags for the article") tags: Option[ArticleTag],
    @(ApiModelProperty @field)(description = "The notes for this draft article") notes: Seq[String],
    @(ApiModelProperty @field)(description = "The users saved for this draft article") users: Seq[String],
    @(ApiModelProperty @field)(description = "The codes from GREP API registered for this draft article") grepCodes: Seq[String])
