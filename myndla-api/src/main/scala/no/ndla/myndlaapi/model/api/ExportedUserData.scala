/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.myndlaapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.myndla.MyNDLAUser
import sttp.tapir.Schema.annotations.description

case class ExportedUserData(
    @description("The users data") userData: MyNDLAUser,
    @description("The users folders") folders: List[Folder]
)

object ExportedUserData {
  implicit def encoder: Encoder[ExportedUserData] = deriveEncoder[ExportedUserData]
  implicit def decoder: Decoder[ExportedUserData] = deriveDecoder[ExportedUserData]
}
