/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.joda.time.DateTime
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

sealed trait SearchResults

@ApiModel(description = "Search result for article api")
case class ArticleResults(
    @(ApiModelProperty @field)(description = "The type of search results (articles)") `type`: String,
    @(ApiModelProperty @field)(description = "The language of the search results") language: String,
    @(ApiModelProperty @field)(description = "The total number of articles matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "The page from which results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The actual search results") results: Seq[ArticleResult])
    extends SearchResults

@ApiModel(description = "Search result for learningpath api")
case class LearningpathResults(
    @(ApiModelProperty @field)(description = "The type of search results (learningpaths)") `type`: String,
    @(ApiModelProperty @field)(description = "The language of the search results") language: String,
    @(ApiModelProperty @field)(description = "The total number of learningpaths matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "The page from which results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The actual search results") results: Seq[LearningpathResult])
    extends SearchResults

@ApiModel(description = "Search result for image api")
case class ImageResults(
    @(ApiModelProperty @field)(description = "The type of search results (images)") `type`: String,
    @(ApiModelProperty @field)(description = "The language of the search results") language: String,
    @(ApiModelProperty @field)(description = "The total number of images matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "The page from which results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The actual search results") results: Seq[ImageResult])
    extends SearchResults

@ApiModel(description = "Search result for audio api")
case class AudioResults(
    @(ApiModelProperty @field)(description = "The type of search results (audios)") `type`: String,
    @(ApiModelProperty @field)(description = "The language of the search results") language: String,
    @(ApiModelProperty @field)(description = "The total number of audios matching this query") totalCount: Long,
    @(ApiModelProperty @field)(description = "The page from which results are shown from") page: Int,
    @(ApiModelProperty @field)(description = "The number of results per page") pageSize: Int,
    @(ApiModelProperty @field)(description = "The actual search results") results: Seq[AudioResult])
    extends SearchResults

@ApiModel(description = "Description of an error when communicating with an api")
case class SearchError(
    @(ApiModelProperty @field)(description = "The api where the error occurred") `type`: String,
    @(ApiModelProperty @field)(description = "An error message describing the error") errorMsg: String)
    extends SearchResults
