/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import no.ndla.common.model.domain.draft.Draft
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props

trait DraftApiClient {
  this: NdlaClient & SearchApiClient & Props =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(val baseUrl: String) extends SearchApiClient[Draft] {
    override val searchPath     = "draft-api/v1/drafts"
    override val name           = "drafts"
    override val dumpDomainPath = "intern/dump/article"
  }

}
