/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.model.api.{MyNDLABundleDTO, SingleResourceStatsDTO}
import no.ndla.common.model.api.config.ConfigMetaRestrictedDTO
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.config.ConfigKey
import no.ndla.common.model.api.myndla as api
import no.ndla.network.NdlaClient
import no.ndla.network.model.NdlaRequest
import sttp.client3.quick.*

import scala.util.{Failure, Success, Try}

trait MyNDLAApiClient {
  this: HasBaseProps & NdlaClient =>

  val myndlaApiClient: MyNDLAApiClient

  class MyNDLAApiClient {
    private val statsEndpoint = s"http://${props.MyNDLAApiHost}/myndla-api/v1/stats"
    private val userEndpoint  = uri"http://${props.MyNDLAApiHost}/myndla-api/v1/users"

    def getUserWithFeideToken(feideToken: String): Try[api.MyNDLAUserDTO] = {
      val req = quickRequest.get(userEndpoint)
      ndlaClient.fetchWithForwardedFeideAuth[api.MyNDLAUserDTO](req, Some(feideToken))
    }

    def isWriteRestricted: Try[Boolean] = {
      doRequest(
        quickRequest.get(
          uri"http://${props.MyNDLAApiHost}/myndla-api/v1/config/${ConfigKey.LearningpathWriteRestricted.entryName}"
        )
      ).flatMap(_.value match {
        case Left(value) => Success(value)
        case _           => Failure(new RuntimeException("Expected boolean, got list"))
      })
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

    private def doRequest(httpRequest: NdlaRequest): Try[ConfigMetaRestrictedDTO] = {
      ndlaClient.fetchWithForwardedAuth[ConfigMetaRestrictedDTO](httpRequest, None)
    }
  }
}
