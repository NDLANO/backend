/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.draft

import no.ndla.searchapi.model.api.Title
import no.ndla.searchapi.model.api.article.{ArticleIntroduction, VisualElement}
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the article")
case class DraftSummary(
    @(ApiModelProperty @field)(description = "The unique id of the article") id: Long,
    @(ApiModelProperty @field)(description = "The title of the article") title: Title,
    @(ApiModelProperty @field)(description = "A visual element article") visualElement: Option[VisualElement],
    @(ApiModelProperty @field)(description = "An introduction for the article") introduction: Option[
      ArticleIntroduction],
    @(ApiModelProperty @field)(
      description = "The full url to where the complete information about the article can be found") url: String,
    @(ApiModelProperty @field)(description = "Describes the license of the article") license: String,
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
    @(ApiModelProperty @field)(description = "A list of available languages for this audio") supportedLanguages: Seq[
      String],
    @(ApiModelProperty @field)(description = "The notes for this draft article") notes: Seq[String])
