/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.config

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.config
import no.ndla.common.model.domain.config.{BooleanValue, StringListValue}
import sttp.tapir.Schema.annotations.description

case class ConfigMetaValueDTO(
    @description("Value to set configuration param to.")
    value: Either[Boolean, List[String]]
)

object ConfigMetaValueDTO {
  import no.ndla.common.implicits._
  implicit def eitherEnc: Encoder[Either[Boolean, List[String]]] = eitherEncoder
  implicit def eitherDec: Decoder[Either[Boolean, List[String]]] = eitherDecoder
  implicit def encoder: Encoder[ConfigMetaValueDTO]              = deriveEncoder
  implicit def decoder: Decoder[ConfigMetaValueDTO]              = deriveDecoder

  def from(value: config.ConfigMetaValue): ConfigMetaValueDTO = {
    value match {
      case BooleanValue(value)    => ConfigMetaValueDTO(Left(value))
      case StringListValue(value) => ConfigMetaValueDTO(Right(value))
    }
  }

  def apply(value: Boolean): ConfigMetaValueDTO      = ConfigMetaValueDTO(Left(value))
  def apply(value: List[String]): ConfigMetaValueDTO = ConfigMetaValueDTO(Right(value))
}
