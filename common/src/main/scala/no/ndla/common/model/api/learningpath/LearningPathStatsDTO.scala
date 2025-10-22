/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.learningpath

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Statistics for learning paths")
case class LearningPathStatsDTO(
    @description("The total number of learning paths in My NDLA")
    numberOfMyNdlaLearningPaths: Long,
    @description("The total number of learning path owners in My NDLA")
    numberOfMyNdlaLearningPathOwners: Long,
)

object LearningPathStatsDTO {
  implicit def encoder: Encoder[LearningPathStatsDTO] = deriveEncoder
  implicit def decoder: Decoder[LearningPathStatsDTO] = deriveDecoder
}
