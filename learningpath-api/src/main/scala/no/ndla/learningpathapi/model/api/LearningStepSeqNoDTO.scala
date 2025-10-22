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

@description("Information about the sequence number for a step")
case class LearningStepSeqNoDTO(
    @description("The sequence number for the learningstep")
    seqNo: Int
)

object LearningStepSeqNoDTO {
  implicit val encoder: Encoder[LearningStepSeqNoDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningStepSeqNoDTO] = deriveDecoder
}
