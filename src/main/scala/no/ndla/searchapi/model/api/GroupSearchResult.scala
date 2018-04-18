/*
 * Part of NDLA search_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Search result for group search")
case class GroupSearchResult(@(ApiModelProperty@field)(description = "The total number of resources matching this query") totalCount: Long,
                             @(ApiModelProperty@field)(description = "Type of resources in this object") resourceType: String,
                             @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                             @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                             @(ApiModelProperty@field)(description = "The chosen search language") language: String,
                             @(ApiModelProperty@field)(description = "The search results") results: Seq[GroupSummary])

@ApiModel(description = "Search result for group search")
case class GroupSummary(@(ApiModelProperty@field)(description = "The unique id of this resource") id: Long,
                        @(ApiModelProperty@field)(description = "The title of the resource") title: Title,
                        @(ApiModelProperty@field)(description = "The url pointing to the resource") url: String)
