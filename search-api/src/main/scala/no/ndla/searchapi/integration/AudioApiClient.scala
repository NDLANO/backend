/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.network.NdlaClient
import no.ndla.searchapi.model.domain.{AudioApiSearchResults, SearchParams}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait AudioApiClient {
  this: NdlaClient with SearchApiClient =>
  val audioApiClient: AudioApiClient

  class AudioApiClient(val baseUrl: String) extends SearchApiClient {
    override val searchPath = "audio-api/v1/audio"
    override val name = "audios"

    def search(searchParams: SearchParams)(
        implicit executionContext: ExecutionContext): Future[Try[AudioApiSearchResults]] =
      search[AudioApiSearchResults](searchParams)
  }
}
