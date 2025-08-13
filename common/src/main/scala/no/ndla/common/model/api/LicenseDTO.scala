/*
 * Part of NDLA common
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Description of license information")
case class LicenseDTO(
    @description("The name of the license") license: String,
    @description("Description of the license") description: Option[String],
    @description("Url to where the license can be found") url: Option[String]
)

object LicenseDTO {
  implicit def encoder: Encoder[LicenseDTO]          = deriveEncoder[LicenseDTO]
  implicit def decoder: Decoder[LicenseDTO]          = deriveDecoder[LicenseDTO]
  implicit def schema: sttp.tapir.Schema[LicenseDTO] = sttp.tapir.Schema.derived
}
