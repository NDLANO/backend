/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */
package no.ndla.myndla.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

case class ExportedUserData(
    @description("The users data") userData: MyNDLAUser,
    @description("The users folders") folders: List[Folder]
)

object ExportedUserData {
  implicit def encoder: Encoder[ExportedUserData] = deriveEncoder[ExportedUserData]
  implicit def decoder: Decoder[ExportedUserData] = deriveDecoder[ExportedUserData]
}
