/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema.annotations.description

@description("Result of subject aggregations")
case class SubjectAggregations(
    subjects: List[SubjectAggregation]
)

object SubjectAggregations {
  implicit val encoder: Encoder[SubjectAggregations] = deriveEncoder
  implicit val decoder: Decoder[SubjectAggregations] = deriveDecoder

}

@description("Aggregations for a single subject'")
case class SubjectAggregation(
    @description("Id of the aggregated subject")
    subjectId: String,
    @description("Number of resources in subject")
    totalArticleCount: Long,
    @description("Number of resources in subject with published older than 5 years")
    oldArticleCount: Long,
    @description("Number of resources in subject with a revision date expiration in one year")
    revisionCount: Long,
    @description("Number of resources in 'flow' (Articles not in `PUBLISHED`, `UNPUBLISHED` or `ARCHIVED` status")
    flowCount: Long,
    @description("Number of favorited resources")
    favoritedCount: Long
)

object SubjectAggregation {
  implicit val encoder: Encoder[SubjectAggregation] = deriveEncoder
  implicit val decoder: Decoder[SubjectAggregation] = deriveDecoder
}
