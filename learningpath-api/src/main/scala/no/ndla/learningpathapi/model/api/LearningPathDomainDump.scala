/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.learningpathapi.model.domain.{LearningPath, LearningStep}
import sttp.tapir.Schema.annotations.description

@description("Information about learningpaths")
case class LearningPathDomainDump(
    @description("The total number of learningpaths in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[LearningPath]
)

object LearningPathDomainDump {
  implicit val encoder: Encoder[LearningPathDomainDump] = deriveEncoder
  implicit val decoder: Decoder[LearningPathDomainDump] = deriveDecoder
}

@description("Information about learningsteps")
case class LearningStepDomainDump(
    @description("The total number of learningsteps in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[LearningStep]
)

object LearningStepDomainDump {
  implicit val encoder: Encoder[LearningStepDomainDump] = deriveEncoder
  implicit val decoder: Decoder[LearningStepDomainDump] = deriveDecoder
}
