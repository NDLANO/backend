/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Short summary of information about the audio")
case class AudioSummary(@(ApiModelProperty @field)(description = "The unique id of the audio") id: Long,
                          @(ApiModelProperty @field)(description = "The title of the audio") titles: Seq[Title],
                          @(ApiModelProperty @field)(description = "The full url to where the complete information about the audio can be found") url: String,
                          @(ApiModelProperty @field)(description = "Describes the license of the audio") license: String
)

@ApiModel(description = "Meta information about the audio object")
case class AudioMetaInformation(@(ApiModelProperty@field)(description = "The unique id of this audio") id: Long,
                                @(ApiModelProperty@field)(description = "The titles of the audio file") titles: Seq[Title],
                                @(ApiModelProperty@field)(description = "The audio files in different languages") audioFiles: Seq[Audio],
                                @(ApiModelProperty@field)(description = "Copyright information for the audio files") copyright: Copyright,
                                @(ApiModelProperty@field)(description = "Tags for this audio file") tags: Seq[Tag])

case class Title(@(ApiModelProperty@field)(description = "The title of the audio file") title: String,
                 @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the title") language: Option[String])

@ApiModel(description = "Url and size information about the image")
case class Audio(@(ApiModelProperty@field)(description = "The path to where the audio is located") url: String,
                 @(ApiModelProperty@field)(description = "The mime type of the audio file") mimeType: String,
                 @(ApiModelProperty@field)(description = "The size of the audio file") fileSize: Long,
                 @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the audio") language: Option[String])

@ApiModel(description = "Description of copyright information")
case class Copyright(@(ApiModelProperty@field)(description = "The license for the audio") license: License,
                     @(ApiModelProperty@field)(description = "Reference to where the audio is procured") origin: Option[String],
                     @(ApiModelProperty@field)(description = "") authors: Seq[Author])

@ApiModel(description = "Description of license information")
case class License(@(ApiModelProperty@field)(description = "The name of the license") license: String,
                   @(ApiModelProperty@field)(description = "Description of the license") description: String,
                   @(ApiModelProperty@field)(description = "Url to where the license can be found") url: Option[String])

case class Author(@(ApiModelProperty@field)(description = "The description of the author. Eg. author or publisher") `type`: String,
                  @(ApiModelProperty@field)(description = "The name of the of the author") name: String)

@ApiModel(description = "Description of the tags of the audio")
case class Tag(@(ApiModelProperty@field)(description = "The searchable tag.") tags: Seq[String],
               @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in tag") language: Option[String])

@ApiModel(description = "Information about search-results")
case class SearchResult(@(ApiModelProperty@field)(description = "The total number of articles matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The search results") results: Seq[AudioSummary])
