/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api.config

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.myndla.model.domain.config
import no.ndla.myndla.model.domain.config.{BooleanValue, StringListValue}
import sttp.tapir.Schema.annotations.description

case class ConfigMetaValue(
    @description("Value to set configuration param to.")
    value: Either[Boolean, List[String]]
)

object ConfigMetaValue {
  import no.ndla.common.implicits._
  implicit def eitherEnc: Encoder[Either[Boolean, List[String]]] = eitherEncoder
  implicit def eitherDec: Decoder[Either[Boolean, List[String]]] = eitherDecoder
  implicit def encoder: Encoder[ConfigMetaValue]                 = deriveEncoder
  implicit def decoder: Decoder[ConfigMetaValue]                 = deriveDecoder

  def from(value: config.ConfigMetaValue): ConfigMetaValue = {
    value match {
      case BooleanValue(value)    => ConfigMetaValue(Left(value))
      case StringListValue(value) => ConfigMetaValue(Right(value))
    }
  }

  def apply(value: Boolean): ConfigMetaValue      = ConfigMetaValue(Left(value))
  def apply(value: List[String]): ConfigMetaValue = ConfigMetaValue(Right(value))
}
