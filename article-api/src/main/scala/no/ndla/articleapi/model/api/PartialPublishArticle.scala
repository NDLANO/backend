/*
 * Part of NDLA article-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import java.time.LocalDateTime
import scala.annotation.meta.field

// format: off
@ApiModel(description = "Partial data about article to publish independently")
case class PartialPublishArticle(
    @(ApiModelProperty @field)(description = "Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: Option[Availability.Value],
    @(ApiModelProperty @field)(description = "A list of codes from GREP API connected to the article") grepCodes: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "The name of the license") license: Option[String],
    @(ApiModelProperty @field)(description = "A list of meta description objects") metaDescription: Option[Seq[ArticleMetaDescription]],
    @(ApiModelProperty @field)(description = "A list of content related to the article") relatedContent: Option[Seq[RelatedContent]],
    @(ApiModelProperty @field)(description = "A list of tag objects") tags: Option[Seq[ArticleTag]],
    @(ApiModelProperty @field)(description = "A revision date to specify expected earliest revision date of the article") revisionDate:  Either[Null, Option[LocalDateTime]],
)
