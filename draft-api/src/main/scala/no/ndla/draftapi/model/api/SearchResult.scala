/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Information about search-results")
case class SearchResult(
    @(ApiModelProperty @field)(description = "The total number of articles matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[ArticleSummary])

@ApiModel(description = "Information and metadata about codes from GREP API")
case class GrepCodesSearchResult(
    @(ApiModelProperty @field)(description = "The total number of codes from GREP API matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[String])

@ApiModel(description = "Information about tags-search-results")
case class TagsSearchResult(
    @(ApiModelProperty @field)(description = "The total number of tags matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[String])

@ApiModel(description = "Information about articles")
case class ArticleDump(
    @(ApiModelProperty @field)(description = "The total number of articles in the database") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[Article])

@ApiModel(description = "Information about agreements")
case class AgreementSearchResult(
    @(ApiModelProperty @field)(description = "The total number of articles matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "For which page results are shown from") page: Option[Int],
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The chosen search language") language: String,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[AgreementSummary])
