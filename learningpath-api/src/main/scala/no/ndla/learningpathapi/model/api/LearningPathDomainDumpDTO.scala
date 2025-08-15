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
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningStep}
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import no.ndla.common.DeriveHelpers

@description("Information about learningpaths")
case class LearningPathDomainDumpDTO(
    @description("The total number of learningpaths in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[LearningPath]
)

object LearningPathDomainDumpDTO {
  implicit val encoder: Encoder[LearningPathDomainDumpDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningPathDomainDumpDTO] = deriveDecoder
  import sttp.tapir.generic.auto.*
  implicit def schema: Schema[LearningPathDomainDumpDTO] = DeriveHelpers.getSchema
}

@description("Information about learningsteps")
case class LearningStepDomainDumpDTO(
    @description("The total number of learningsteps in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[LearningStep]
)

object LearningStepDomainDumpDTO {
  implicit val encoder: Encoder[LearningStepDomainDumpDTO] = deriveEncoder
  implicit val decoder: Decoder[LearningStepDomainDumpDTO] = deriveDecoder
}
