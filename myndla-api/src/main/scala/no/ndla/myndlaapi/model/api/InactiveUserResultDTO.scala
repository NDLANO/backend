/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

case class InactiveUserResultDTO(numberOfUsersDeleted: Int, numberOfUsersEmailed: Int)

object InactiveUserResultDTO {
  implicit def encoder: Encoder[InactiveUserResultDTO] = deriveEncoder[InactiveUserResultDTO]
  implicit def decoder: Decoder[InactiveUserResultDTO] = deriveDecoder[InactiveUserResultDTO]
}
