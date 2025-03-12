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
case class StatsDTO(
    @description("The total number of users registered ") numberOfUsers: Long,
    @description("The total number of created folders") numberOfFolders: Long,
    @description("The total number of favourited resources ") numberOfResources: Long,
    @description("The total number of created tags") numberOfTags: Long,
    @description("The total number of favourited subjects") numberOfSubjects: Long,
    @description("The total number of shared folders") numberOfSharedFolders: Long,
    @description("The total number of learning paths in My NDLA") numberOfMyNdlaLearningPaths: Long,
    @description("Stats for type resources") favouritedResources: List[ResourceStatsDTO],
    @description("Stats for favourited resources") favourited: Map[String, Long],
    @description("Stats for the users") users: UserStatsDTO
)

object StatsDTO {
  implicit def encoder: Encoder[StatsDTO] = deriveEncoder
  implicit def decoder: Decoder[StatsDTO] = deriveDecoder
}

case class ResourceStatsDTO(
    @description("The type of favourited resouce") `type`: String,
    @description("The number of favourited resource") number: Long
)

object ResourceStatsDTO {
  implicit def encoder: Encoder[ResourceStatsDTO] = deriveEncoder
  implicit def decoder: Decoder[ResourceStatsDTO] = deriveDecoder
}

case class UserStatsDTO(
    @description("The total number of users") total: Long,
    @description("The number of employees") employees: Long,
    @description("The number of students") students: Long,
    @description("The number of users with favourites") withFavourites: Long,
    @description("The number of users with no favourites") noFavourites: Long,
    @description("The number of users in the arena") arena: Long
)

object UserStatsDTO {
  implicit def encoder: Encoder[UserStatsDTO] = deriveEncoder
  implicit def decoder: Decoder[UserStatsDTO] = deriveDecoder
}
