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
import no.ndla.common.model.NDLADate
import no.ndla.myndlaapi.model.domain.{RobotDefinition, RobotStatus}
import sttp.tapir.Schema.annotations.description

@description("DTO for creating a new robot definition")
case class RobotDefinitionDTO(
    @description("The unique identifier of the robot")
    id: String,
    @description("The status of the robot")
    status: RobotStatus,
    @description("The configuration details of the robot")
    configuration: RobotConfigurationDTO,
    @description("The date when the robot was created")
    created: NDLADate,
    @description("The date when the robot was last updated")
    updated: NDLADate,
    @description("The date when the robot was shared, if applicable")
    shared: Option[NDLADate],
)

object RobotDefinitionDTO {
  implicit val encoder: Encoder[RobotDefinitionDTO] = deriveEncoder[RobotDefinitionDTO]
  implicit val decoder: Decoder[RobotDefinitionDTO] = deriveDecoder[RobotDefinitionDTO]

  def fromDomain(domain: RobotDefinition): RobotDefinitionDTO = {
    RobotDefinitionDTO(
      id = domain.id.toString,
      status = domain.status,
      created = domain.created,
      updated = domain.updated,
      shared = domain.shared,
      configuration = RobotConfigurationDTO(
        title = domain.configuration.title,
        version = domain.configuration.version,
        settings = RobotSettingsDTO(
          name = domain.configuration.settings.name,
          systemprompt = domain.configuration.settings.systemprompt,
          question = domain.configuration.settings.question,
          temperature = domain.configuration.settings.temperature,
          model = domain.configuration.settings.model,
        ),
      ),
    )
  }
}
