/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props

class ArticleApiHttpClient(val baseUrl: String)(using ndlaClient: NdlaClient, props: Props) extends ArticleApiClient {
  override val searchPath     = "article-api/v2/articles"
  override val name           = "articles"
  override val dumpDomainPath = "intern/dump/article"
}
