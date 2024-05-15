/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient

trait DraftConceptApiClient {
  this: NdlaClient & SearchApiClient =>
  val draftConceptApiClient: DraftConceptApiClient

  class DraftConceptApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath     = "concept-api/v1/drafts"
    override val name           = "concepts"
    override val dumpDomainPath = "intern/dump/draft-concept"
  }
}
