/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class StatusDTO(
    @description("The current status of the article") current: String,
    @description("Previous statuses this article has been in") other: Seq[String]
)

object StatusDTO {
  implicit def encoder: Encoder[StatusDTO] = deriveEncoder
  implicit def decoder: Decoder[StatusDTO] = deriveDecoder
}
