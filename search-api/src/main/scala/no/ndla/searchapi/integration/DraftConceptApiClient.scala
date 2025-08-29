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
import no.ndla.searchapi.Props

class DraftConceptApiClient(val baseUrl: String)(using ndlaClient: NdlaClient, props: Props)
    extends SearchApiClient[Concept] {
  override val searchPath     = "concept-api/v1/drafts"
  override val name           = "concepts"
  override val dumpDomainPath = "intern/dump/draft-concept"
}
