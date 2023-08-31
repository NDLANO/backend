/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import no.ndla.searchapi.model.domain.{ArticleApiSearchResults, SearchParams}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait DraftApiClient {
  this: NdlaClient with SearchApiClient with Props =>
  val draftApiClient: DraftApiClient

  class DraftApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath     = "draft-api/v1/drafts"
    override val name           = "articles"
    override val dumpDomainPath = "intern/dump/article"

    def search(searchParams: SearchParams)(implicit
        executionContext: ExecutionContext
    ): Future[Try[ArticleApiSearchResults]] =
      search[ArticleApiSearchResults](searchParams)
  }

}
