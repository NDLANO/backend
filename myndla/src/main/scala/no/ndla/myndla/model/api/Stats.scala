/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
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
)

object Stats {
  implicit def encoder: Encoder[Stats] = deriveEncoder
  implicit def decoder: Decoder[Stats] = deriveDecoder
}

case class ResourceStats(
    @(ApiModelProperty @field)(description = "The type of favourited resouce") `type`: String,
    @(ApiModelProperty @field)(description = "The number of favourited resource") number: Long
)

object ResourceStats {
  implicit def encoder: Encoder[ResourceStats] = deriveEncoder
  implicit def decoder: Decoder[ResourceStats] = deriveDecoder
}
