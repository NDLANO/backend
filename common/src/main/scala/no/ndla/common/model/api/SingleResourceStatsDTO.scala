/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Stats for single resource")
case class SingleResourceStatsDTO(
    @description("The resource type") resourceType: String,
    @description("Id of the resource") id: String,
    @description("The number of times the resource has been favorited") favourites: Long
)

object SingleResourceStatsDTO {
  implicit val encoder: Encoder[SingleResourceStatsDTO] = deriveEncoder
  implicit val decoder: Decoder[SingleResourceStatsDTO] = deriveDecoder
}
