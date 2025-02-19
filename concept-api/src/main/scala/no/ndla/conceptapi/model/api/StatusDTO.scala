/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Status information for the concept")
case class StatusDTO(
    @description("The current status of the concept") current: String,
    @description("Previous statuses this concept has been in") other: Seq[String]
)

object StatusDTO {
  implicit val encoder: Encoder[StatusDTO] = deriveEncoder
  implicit val decoder: Decoder[StatusDTO] = deriveDecoder
}
