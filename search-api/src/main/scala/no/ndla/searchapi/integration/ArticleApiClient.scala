/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient

trait ArticleApiClient {
  this: NdlaClient & SearchApiClient =>
  val articleApiClient: ArticleApiClient

  class ArticleApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath     = "article-api/v2/articles"
    override val name           = "articles"
    override val dumpDomainPath = "intern/dump/article"
  }
}
