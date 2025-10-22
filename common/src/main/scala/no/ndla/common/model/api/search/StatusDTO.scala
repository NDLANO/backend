/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.search

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Status information of the resource")
case class StatusDTO(
    @description("The current status of the resource")
    current: String,
    @description("Previous statuses this resource has been in")
    other: Seq[String],
)

object StatusDTO {
  implicit val encoder: Encoder[StatusDTO] = deriveEncoder
  implicit val decoder: Decoder[StatusDTO] = deriveDecoder
}
