/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import no.ndla.common.model.domain.concept.Concept
import no.ndla.network.NdlaClient

trait DraftConceptApiClient {
  this: NdlaClient & SearchApiClient =>
  lazy val draftConceptApiClient: DraftConceptApiClient

  class DraftConceptApiClient(val baseUrl: String) extends SearchApiClient[Concept] {
    override val searchPath     = "concept-api/v1/drafts"
    override val name           = "concepts"
    override val dumpDomainPath = "intern/dump/draft-concept"
  }
}
