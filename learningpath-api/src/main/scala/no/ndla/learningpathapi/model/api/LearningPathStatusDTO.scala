/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.learningpath.LearningPathStatus
import sttp.tapir.Schema.annotations.description

@description("Status information about a learningpath")
case class LearningPathStatusDTO(
    @description("The publishing status of the learningpath") status: LearningPathStatus
)

object LearningPathStatusDTO {
  implicit val encoder: Encoder[LearningPathStatusDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningPathStatusDTO] = deriveDecoder
}
