/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.api.{MyNDLABundleDTO, SingleResourceStatsDTO}
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.api.myndla as api
import no.ndla.network.NdlaClient
import sttp.client3.quick.*

import scala.util.{Success, Try}

class MyNDLAApiClient(using props: BaseProps, ndlaClient: NdlaClient) {
  private val statsEndpoint = s"http://${props.MyNDLAApiHost}/myndla-api/v1/stats"
  private val userEndpoint  = uri"http://${props.MyNDLAApiHost}/myndla-api/v1/users"

  def getUserWithFeideToken(feideToken: String): Try[api.MyNDLAUserDTO] = {
    val req = quickRequest.get(userEndpoint)
    ndlaClient.fetchWithForwardedFeideAuth[api.MyNDLAUserDTO](req, Some(feideToken))
  }

  def isWriteRestricted: Try[Boolean] = {
    Success(false)
  }

  def getStatsFor(id: String, resourceTypes: List[ResourceType]): Try[List[SingleResourceStatsDTO]] = {
    val url = uri"$statsEndpoint/favorites/${resourceTypes.map(_.toString).mkString(",")}/$id"
    val req = quickRequest.get(url)
    ndlaClient.fetch[List[SingleResourceStatsDTO]](req)
  }

  def getMyNDLABundle: Try[MyNDLABundleDTO] = {
    val url = uri"$statsEndpoint/favorites"
    val req = quickRequest.get(url)
    val res = ndlaClient.fetch[Map[String, Map[String, Long]]](req)
    res.map(favMap => MyNDLABundleDTO(favMap))
  }
}
