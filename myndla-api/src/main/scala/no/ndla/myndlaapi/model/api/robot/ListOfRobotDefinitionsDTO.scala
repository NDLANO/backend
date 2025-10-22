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

@description("DTO for listing all robot definitions")
case class ListOfRobotDefinitionsDTO(robots: List[RobotDefinitionDTO])

object ListOfRobotDefinitionsDTO {
  implicit val encoder: Encoder[ListOfRobotDefinitionsDTO] = deriveEncoder[ListOfRobotDefinitionsDTO]
  implicit val decoder: Decoder[ListOfRobotDefinitionsDTO] = deriveDecoder[ListOfRobotDefinitionsDTO]
}
