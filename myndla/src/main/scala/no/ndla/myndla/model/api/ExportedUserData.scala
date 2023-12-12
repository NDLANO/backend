/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */
package no.ndla.myndla.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.scalatra.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class ExportedUserData(
    @(ApiModelProperty @field)(description = "The users data") userData: MyNDLAUser,
    @(ApiModelProperty @field)(description = "The users folders") folders: List[Folder]
)

object ExportedUserData {
  implicit def encoder: Encoder[ExportedUserData] = deriveEncoder[ExportedUserData]
  implicit def decoder: Decoder[ExportedUserData] = deriveDecoder[ExportedUserData]
}
