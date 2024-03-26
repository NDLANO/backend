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

case class Breadcrumb(
    @description("UUID of the folder") id: String,
    @description("Folder name") name: String
)

object Breadcrumb {
  implicit val encoder: Encoder[Breadcrumb] = deriveEncoder[Breadcrumb]
  implicit val decoder: Decoder[Breadcrumb] = deriveDecoder[Breadcrumb]
}
