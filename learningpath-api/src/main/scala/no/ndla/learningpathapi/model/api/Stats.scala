/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Stats for my-ndla usage")
case class Stats(
    @(ApiModelProperty @field)(description = "The total number of users registered ") numberOfUsers: Long,
    @(ApiModelProperty @field)(description = "The total number of created folders") numberOfFolders: Long,
    @(ApiModelProperty @field)(description = "The total number of favourited resources ") numberOfResources: Long,
    @(ApiModelProperty @field)(description = "The total number of created tags") numberOfTags: Long,
    @(ApiModelProperty @field)(description = "The total number of favourited subjects") numberOfSubjects: Long,
    @(ApiModelProperty @field)(description = "The total number of shared folders") numberOfSharedFolders: Long,
    @(ApiModelProperty @field)(description = "Stats for type resources") favouritedResources: List[ResourceStats]
) {}

case class ResourceStats(
    @(ApiModelProperty @field)(description = "The type of favourited resouce") `type`: String,
    @(ApiModelProperty @field)(description = "The number of favourited resource") number: Long
) {}
