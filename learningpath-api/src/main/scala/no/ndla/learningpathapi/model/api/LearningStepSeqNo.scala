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
case class LearningStepSeqNo(
    @description("The sequence number for the learningstep") seqNo: Int
)

object LearningStepSeqNo {
  implicit val encoder: Encoder[LearningStepSeqNo] = deriveEncoder
  implicit val decoder: Decoder[LearningStepSeqNo] = deriveDecoder
}
