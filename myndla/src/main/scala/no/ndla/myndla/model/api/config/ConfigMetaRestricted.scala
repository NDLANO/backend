/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api.config

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Describes configuration value.")
case class ConfigMetaRestricted(
    @description("Configuration key") key: String,
    @description("Configuration value.") value: Either[Boolean, List[String]]
)

object ConfigMetaRestricted {
  import no.ndla.common.implicits._
  implicit def eitherEnc: Encoder[Either[Boolean, List[String]]] = eitherEncoder
  implicit def eitherDec: Decoder[Either[Boolean, List[String]]] = eitherDecoder
  implicit def encoder: Encoder[ConfigMetaRestricted]            = deriveEncoder
  implicit def decoder: Decoder[ConfigMetaRestricted]            = deriveDecoder
}
