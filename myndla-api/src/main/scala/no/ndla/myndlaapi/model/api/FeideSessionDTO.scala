/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class FeideSessionDTO(accessToken: String)

object FeideSessionDTO {
  implicit val encoder: Encoder[FeideSessionDTO] = deriveEncoder[FeideSessionDTO]
  implicit val decoder: Decoder[FeideSessionDTO] = deriveDecoder[FeideSessionDTO]
}
