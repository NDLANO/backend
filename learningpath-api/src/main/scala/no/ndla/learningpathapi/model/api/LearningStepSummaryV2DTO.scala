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

@description("Summary of meta information for a learningstep")
case class LearningStepSummaryV2DTO(
    @description("The id of the learningstep")
    id: Long,
    @description("The sequence number for the step. The first step has seqNo 0.")
    seqNo: Int,
    @description("The title of the learningstep")
    title: TitleDTO,
    @description("The type of the step")
    `type`: String,
    @description("The full url to where the complete metainformation about the learningstep can be found")
    metaUrl: String
)

object LearningStepSummaryV2DTO {
  implicit val encoder: Encoder[LearningStepSummaryV2DTO] = deriveEncoder
  implicit val decoder: Decoder[LearningStepSummaryV2DTO] = deriveDecoder
}
