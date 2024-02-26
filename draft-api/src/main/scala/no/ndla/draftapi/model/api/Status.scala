/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class Status(
    @description("The current status of the article") current: String,
    @description("Previous statuses this article has been in") other: Seq[String]
)

object Status {
  implicit def encoder: Encoder[Status] = deriveEncoder
  implicit def decoder: Decoder[Status] = deriveDecoder
}
