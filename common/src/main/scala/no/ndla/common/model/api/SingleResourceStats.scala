/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Stats for single resource")
case class SingleResourceStats(
    @description("Id of the resource") id: String,
    @description("The number of times the resource has been favorited") favourites: Long
)

object SingleResourceStats {
  implicit val encoder: Encoder[SingleResourceStats] = deriveEncoder
  implicit val decoder: Decoder[SingleResourceStats] = deriveDecoder
}
