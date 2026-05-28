/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class FeideIdToken(
    iss: String,
    jti: String,
    aud: List[String],
    sub: String,
    iat: Long,
    exp: Long,
    email: String,
    name: String,
    userid_sec: List[String],
    eduPersonPrincipalName: String,
    originalToken: String,
)

object FeideIdToken {
  implicit val encoder: Encoder[FeideIdToken] = deriveEncoder
  implicit val decoder: Decoder[FeideIdToken] = deriveDecoder
}
