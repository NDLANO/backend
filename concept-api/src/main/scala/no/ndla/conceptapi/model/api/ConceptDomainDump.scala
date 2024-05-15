/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.concept.Concept as DomainConcept
import sttp.tapir.Schema.annotations.description

@description("Information about articles")
case class ConceptDomainDump(
    @description("The total number of concepts in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[DomainConcept]
)

object ConceptDomainDump {
  implicit val encoder: Encoder[ConceptDomainDump] = deriveEncoder
  implicit val decoder: Decoder[ConceptDomainDump] = deriveDecoder
}
