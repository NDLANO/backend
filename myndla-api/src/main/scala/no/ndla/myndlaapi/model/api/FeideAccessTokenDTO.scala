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

case class FeideAccessTokenDTO(accessToken: String)

object FeideAccessTokenDTO {
  implicit val encoder: Encoder[FeideAccessTokenDTO] = deriveEncoder[FeideAccessTokenDTO]
  implicit val decoder: Decoder[FeideAccessTokenDTO] = deriveDecoder[FeideAccessTokenDTO]
}
