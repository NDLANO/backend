/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.learningpath.LearningPathStatus
import sttp.tapir.Schema.annotations.description

// format: off
@description("Status information about a learningpath")
case class UpdateLearningPathStatusDTO(
    @description("The publishing status of the learningpath") status: LearningPathStatus,
    @description("Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath") message: Option[String]
)

object UpdateLearningPathStatusDTO {
  implicit val encoder: Encoder[UpdateLearningPathStatusDTO] = deriveEncoder
  implicit val decoder: Decoder[UpdateLearningPathStatusDTO] = deriveDecoder
}
