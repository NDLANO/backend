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
import no.ndla.myndlaapi.model.domain.RobotStatus
import sttp.tapir.Schema.annotations.description

@description("DTO for creating a new robot definition")
case class CreateRobotDefinitionDTO(
    status: RobotStatus,
    configuration: RobotConfigurationDTO
)

object CreateRobotDefinitionDTO {
  implicit val encoder: Encoder[CreateRobotDefinitionDTO] = deriveEncoder[CreateRobotDefinitionDTO]
  implicit val decoder: Decoder[CreateRobotDefinitionDTO] = deriveDecoder[CreateRobotDefinitionDTO]
}
