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
import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import sttp.tapir.Schema.annotations.description

case class ExportedUserDataDTO(
    @description("The users data") userData: MyNDLAUserDTO,
    @description("The users folders") folders: List[FolderDTO]
)

object ExportedUserDataDTO {
  implicit def encoder: Encoder[ExportedUserDataDTO] = deriveEncoder[ExportedUserDataDTO]
  implicit def decoder: Decoder[ExportedUserDataDTO] = deriveDecoder[ExportedUserDataDTO]
}
