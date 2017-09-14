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


@ApiModel(description = "Search result for article api")
case class ArticleResult(@(ApiModelProperty@field)(description = "The unique id of this article") id: Long,
                         @(ApiModelProperty@field)(description = "The title of the article") title: String,
                         @(ApiModelProperty@field)(description = "The introduction of the article") introduction: Option[String],
                         @(ApiModelProperty@field)(description = "The type of the article") articleType: String
                        )
