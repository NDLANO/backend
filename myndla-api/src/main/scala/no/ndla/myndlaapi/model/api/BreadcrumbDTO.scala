/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class BreadcrumbDTO(
    @description("UUID of the folder") id: String,
    @description("Folder name") name: String
)

object BreadcrumbDTO {
  implicit val encoder: Encoder[BreadcrumbDTO] = deriveEncoder[BreadcrumbDTO]
  implicit val decoder: Decoder[BreadcrumbDTO] = deriveDecoder[BreadcrumbDTO]
}