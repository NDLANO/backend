/*
 * Part of NDLA common.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.config

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

@description("Describes configuration value.")
case class ConfigMeta(
    @description("Configuration key") key: String,
    @description("Configuration value.") value: Either[Boolean, List[String]],
    @description("Date of when configuration was last updated") updatedAt: NDLADate,
    @description("UserId of who last updated the configuration parameter.") updatedBy: String
)

object ConfigMeta {
  import no.ndla.common.implicits._
  implicit def eitherEnc: Encoder[Either[Boolean, List[String]]] = eitherEncoder
  implicit def eitherDec: Decoder[Either[Boolean, List[String]]] = eitherDecoder
  implicit def encoder: Encoder[ConfigMeta]                      = deriveEncoder
  implicit def decoder: Decoder[ConfigMeta]                      = deriveDecoder
}
