/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.common.model.api.SingleResourceStats
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import sttp.client3.quick.*

trait MyNDLAApiClient {
  this: NdlaClient with Props =>

  val myndlaapiClient: MyNDLAApiClient

  class MyNDLAApiClient {
    props.MyNDLAApiHost
    val statsEndpoint = s"http://${props.MyNDLAApiHost}/myndla-api/v1/stats"

    def getStatsFor(id: String, resourceTypes: List[String]) = {
      val url = uri"$statsEndpoint/${resourceTypes.mkString(",")}/$id"
      val req = quickRequest.get(url)
      ndlaClient.fetch[List[SingleResourceStats]](req)
      val res = simpleHttpClient.send(req)
    }

  }
}
