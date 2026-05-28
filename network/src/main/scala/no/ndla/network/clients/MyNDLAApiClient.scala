/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.clients

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.api.{MyNDLABundleDTO, SingleResourceStatsDTO}
import no.ndla.common.model.domain.ResourceType
import no.ndla.network.NdlaClient
import no.ndla.network.model.*
import sttp.client4.quick.*

import scala.util.{Failure, Success, Try}

class MyNDLAApiClient(using props: BaseProps, ndlaClient: NdlaClient) extends MyNDLAProvider with StrictLogging {
  private val statsEndpoint  = s"http://${props.MyNDLAApiHost}/myndla-api/v1/stats"
  private val internEndpoint = uri"http://${props.MyNDLAApiHost}/intern"

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

  override def getFeideUserWrapperFromIdToken(idToken: FeideIdToken): Either[AuthException, FeideUserWrapper] = {
    val url = uri"$internEndpoint/get-user"
    val req = quickRequest.post(url).body(CirceUtil.toJsonString(idToken))
    ndlaClient.fetch[Option[FeideUserWrapper]](req) match {
      case Success(Some(userWrapper)) => Right(userWrapper)
      case Success(None)              => Left(MissingFeideAccessTokenException())
      case Failure(ex)                => Left(GetFeideUserWrapperException(ex))
    }
  }
}

trait MyNDLAProvider {
  def getFeideUserWrapperFromIdToken(idToken: FeideIdToken): Either[AuthException, FeideUserWrapper]
}
