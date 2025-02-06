/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import sttp.tapir.Schema.annotations.description

sealed trait SearchResultsDTO

@description("Search result for article api")
case class ArticleResultsDTO(
    @description("The type of search results (articles)") `type`: String,
    @description("The language of the search results") language: String,
    @description("The total number of articles matching this query") totalCount: Long,
    @description("The page from which results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The actual search results") results: Seq[ArticleResultDTO]
) extends SearchResultsDTO

@description("Search result for learningpath api")
case class LearningpathResultsDTO(
    @description("The type of search results (learningpaths)") `type`: String,
    @description("The language of the search results") language: String,
    @description("The total number of learningpaths matching this query") totalCount: Long,
    @description("The page from which results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The actual search results") results: Seq[LearningpathResultDTO]
) extends SearchResultsDTO

@description("Search result for image api")
case class ImageResultsDTO(
    @description("The type of search results (images)") `type`: String,
    @description("The language of the search results") language: String,
    @description("The total number of images matching this query") totalCount: Long,
    @description("The page from which results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The actual search results") results: Seq[ImageResultDTO]
) extends SearchResultsDTO

@description("Search result for audio api")
case class AudioResultsDTO(
    @description("The type of search results (audios)") `type`: String,
    @description("The language of the search results") language: String,
    @description("The total number of audios matching this query") totalCount: Long,
    @description("The page from which results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The actual search results") results: Seq[AudioResultDTO]
) extends SearchResultsDTO
