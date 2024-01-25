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
import no.ndla.common.model.NDLADate

@ApiModel(description = "Describes configuration value.")
case class ConfigMeta(
    @ApiModelProperty(description = "Configuration key") key: String,
    @ApiModelProperty(description = "Configuration value.") value: Either[Boolean, List[String]],
    @ApiModelProperty(description = "Date of when configuration was last updated") updatedAt: NDLADate,
    @ApiModelProperty(description = "UserId of who last updated the configuration parameter.") updatedBy: String
)

object ConfigMeta {
  import no.ndla.common.implicits._
  implicit def eitherEnc: Encoder[Either[Boolean, List[String]]] = eitherEncoder
  implicit def eitherDec: Decoder[Either[Boolean, List[String]]] = eitherDecoder
  implicit def encoder: Encoder[ConfigMeta]                      = deriveEncoder
  implicit def decoder: Decoder[ConfigMeta]                      = deriveDecoder
}
