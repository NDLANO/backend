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
import sttp.tapir.Schema.annotations.description

@description("Status information about a learningpath")
case class LearningPathStatus(
    @description("The publishing status of the learningpath") status: String
)

object LearningPathStatus {
  implicit val encoder: Encoder[LearningPathStatus] = deriveEncoder
  implicit val decoder: Decoder[LearningPathStatus] = deriveDecoder
}
