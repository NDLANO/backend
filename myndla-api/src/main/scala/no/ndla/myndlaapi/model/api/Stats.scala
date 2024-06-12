/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Stats for my-ndla usage")
case class Stats(
    @description("The total number of users registered ") numberOfUsers: Long,
    @description("The total number of created folders") numberOfFolders: Long,
    @description("The total number of favourited resources ") numberOfResources: Long,
    @description("The total number of created tags") numberOfTags: Long,
    @description("The total number of favourited subjects") numberOfSubjects: Long,
    @description("The total number of shared folders") numberOfSharedFolders: Long,
    @description("Stats for type resources") favouritedResources: List[ResourceStats],
    @description("Stats for favourited resources") favourited: Map[String, Long]
)

object Stats {
  implicit def encoder: Encoder[Stats] = deriveEncoder
  implicit def decoder: Decoder[Stats] = deriveDecoder
}

case class ResourceStats(
    @description("The type of favourited resouce") `type`: String,
    @description("The number of favourited resource") number: Long
)

object ResourceStats {
  implicit def encoder: Encoder[ResourceStats] = deriveEncoder
  implicit def decoder: Decoder[ResourceStats] = deriveDecoder
}
