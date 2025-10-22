/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api.robot

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("DTO for robot configuration")
case class RobotConfigurationDTO(title: String, version: String, settings: RobotSettingsDTO)

object RobotConfigurationDTO {
  implicit val encoder: Encoder[RobotConfigurationDTO] = deriveEncoder[RobotConfigurationDTO]
  implicit val decoder: Decoder[RobotConfigurationDTO] = deriveDecoder[RobotConfigurationDTO]
}

@description("DTO for robot settings")
case class RobotSettingsDTO(
    name: String,
    systemprompt: Option[String],
    question: Option[String],
    temperature: String,
    model: String,
)

object RobotSettingsDTO {
  implicit val encoder: Encoder[RobotSettingsDTO] = deriveEncoder[RobotSettingsDTO]
  implicit val decoder: Decoder[RobotSettingsDTO] = deriveDecoder[RobotSettingsDTO]
}
