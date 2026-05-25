/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props

class DraftApiHttpClient(val baseUrl: String)(using ndlaClient: NdlaClient, props: Props) extends DraftApiClient {
  override val searchPath     = "draft-api/v1/drafts"
  override val name           = "drafts"
  override val dumpDomainPath = "intern/dump/article"
}
