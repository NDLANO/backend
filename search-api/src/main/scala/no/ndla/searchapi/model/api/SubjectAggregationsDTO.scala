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
case class SubjectAggregationsDTO(subjects: List[SubjectAggregationDTO])

object SubjectAggregationsDTO {
  implicit val encoder: Encoder[SubjectAggregationsDTO] = deriveEncoder
  implicit val decoder: Decoder[SubjectAggregationsDTO] = deriveDecoder

}

@description("Aggregations for a single subject'")
case class SubjectAggregationDTO(
    @description("Id of the aggregated subject")
    subjectId: String,
    @description("Number of resources in subject")
    publishedArticleCount: Long,
    @description("Number of resources in subject with published older than 5 years")
    oldArticleCount: Long,
    @description("Number of resources in subject with a revision date expiration in one year")
    revisionCount: Long,
    @description("Number of resources in 'flow' (Articles not in `PUBLISHED`, `UNPUBLISHED` or `ARCHIVED` status")
    flowCount: Long,
    @description("Number of favorited resources")
    favoritedCount: Long,
)

object SubjectAggregationDTO {
  implicit val encoder: Encoder[SubjectAggregationDTO] = deriveEncoder
  implicit val decoder: Decoder[SubjectAggregationDTO] = deriveDecoder
}
