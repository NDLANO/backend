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
import no.ndla.common.model.domain.myndla.MyNDLAUser

case class FeideUserWrapper(user: MyNDLAUser, idToken: FeideIdToken, accessToken: FeideAccessToken)

object FeideUserWrapper {
  implicit val encoder: Encoder[FeideUserWrapper] = deriveEncoder
  implicit val decoder: Decoder[FeideUserWrapper] = deriveDecoder
}
