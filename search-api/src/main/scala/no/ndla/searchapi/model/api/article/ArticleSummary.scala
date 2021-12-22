/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.api.{MetaDescription, Title}
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the article")
case class ArticleSummary(
    @(ApiModelProperty @field)(description = "The unique id of the article") id: Long,
    @(ApiModelProperty @field)(description = "The title of the article") title: Title,
    @(ApiModelProperty @field)(description = "A visual element article") visualElement: Option[VisualElement],
    @(ApiModelProperty @field)(description = "An introduction for the article") introduction: Option[
      ArticleIntroduction],
    @(ApiModelProperty @field)(description = "A metaDescription for the article") metaDescription: Option[
      MetaDescription],
    @(ApiModelProperty @field)(description = "A meta image for the article") metaImage: Option[ArticleMetaImage],
    @(ApiModelProperty @field)(
      description = "The full url to where the complete information about the article can be found") url: String,
    @(ApiModelProperty @field)(description = "Describes the license of the article") license: String,
    @(ApiModelProperty @field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
    @(ApiModelProperty @field)(description = "A list of available languages for this article") supportedLanguages: Seq[
      String])
