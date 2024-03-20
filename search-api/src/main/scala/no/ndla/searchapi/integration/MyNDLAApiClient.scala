/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.integration

import no.ndla.common.model.api.{MyNDLABundle, SingleResourceStats}
import no.ndla.network.NdlaClient
import no.ndla.searchapi.Props
import sttp.client3.quick.*

import scala.util.Try

trait MyNDLAApiClient {
  this: NdlaClient with Props =>

  val myndlaapiClient: MyNDLAApiClient

  class MyNDLAApiClient {
    private val statsEndpoint = s"http://${props.MyNDLAApiHost}/myndla-api/v1/stats"

    def getStatsFor(id: String, resourceTypes: List[String]): Try[List[SingleResourceStats]] = {
      val url = uri"$statsEndpoint/favorites/${resourceTypes.mkString(",")}/$id"
      val req = quickRequest.get(url)
      ndlaClient.fetch[List[SingleResourceStats]](req)
    }

    def getMyNDLABundle: Try[MyNDLABundle] = {
      val url = uri"$statsEndpoint/favorites"
      val req = quickRequest.get(url)
      val res = ndlaClient.fetch[Map[String, Map[String, Long]]](req)
      res.map(favMap => MyNDLABundle(favMap))
    }

  }
}
