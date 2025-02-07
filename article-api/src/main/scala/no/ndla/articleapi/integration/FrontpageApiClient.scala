/*
 * Part of NDLA article-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import com.typesafe.scalalogging.StrictLogging
import no.ndla.articleapi.Props
import no.ndla.articleapi.service.ConverterService
import no.ndla.common.model.api.FrontPageDTO
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.util.Try

trait FrontpageApiClient {
  this: NdlaClient & ConverterService & Props =>
  val frontpageApiClient: FrontpageApiClient

  class FrontpageApiClient(FrontpageApiBaseUrl: String = props.FrontpageApiUrl) extends StrictLogging {

    private val frontpageApiBaseUrl = s"$FrontpageApiBaseUrl/frontpage-api/v1/frontpage"

    def getFrontpage: Try[FrontPageDTO] = {
      val req = quickRequest.get(uri"$frontpageApiBaseUrl")
      ndlaClient.fetch[FrontPageDTO](req)
    }
  }

}
