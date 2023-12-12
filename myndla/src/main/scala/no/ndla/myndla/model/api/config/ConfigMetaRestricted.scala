/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api.config

import io.circe.{Decoder, Encoder}
import org.scalatra.swagger.annotations.ApiModelProperty
import org.scalatra.swagger.runtime.annotations.ApiModel
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.annotation.meta.field

@ApiModel(description = "Describes configuration value.")
case class ConfigMetaRestricted(
    @(ApiModelProperty @field)(description = "Configuration key") key: String,
    @(ApiModelProperty @field)(description = "Configuration value.") value: Either[Boolean, List[String]]
)

object ConfigMetaRestricted {
  import no.ndla.common.implicits._
  implicit def eitherEnc: Encoder[Either[Boolean, List[String]]] = eitherEncoder
  implicit def eitherDec: Decoder[Either[Boolean, List[String]]] = eitherDecoder
  implicit def encoder: Encoder[ConfigMetaRestricted]            = deriveEncoder
  implicit def decoder: Decoder[ConfigMetaRestricted]            = deriveDecoder
}
