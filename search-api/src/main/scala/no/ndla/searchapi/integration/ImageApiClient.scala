/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.{ImageApiSearchResults, SearchParams}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait ImageApiClient {
  this: NdlaClient with SearchApiClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "image-api/v2/images"
    override val name = "images"

    def search(searchParams: SearchParams)(
        implicit executionContext: ExecutionContext): Future[Try[ImageApiSearchResults]] =
      search[ImageApiSearchResults](searchParams)
  }
}
