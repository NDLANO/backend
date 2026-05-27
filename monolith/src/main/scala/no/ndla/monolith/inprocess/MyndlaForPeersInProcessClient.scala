/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.common.configuration.BaseProps
import no.ndla.common.model.api.{MyNDLABundleDTO, SingleResourceStatsDTO}
import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.MyNDLAUser
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient

import scala.util.Try

/** In-process implementation of [[MyNDLAApiClient]] that delegates to myndla-api's UserService and FolderReadService
  * directly, skipping the HTTP/JSON ser-de hop. Used by every peer per-app `ComponentRegistry` in the monolith.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the peer CRs and
  * [[no.ndla.myndlaapi.ComponentRegistry]].
  *
  * Subclasses [[MyNDLAApiClient]] (rather than just implementing [[no.ndla.network.clients.MyNDLAProvider]]) because
  * peer-app controllers and services take the concrete client type as a dependency; the HTTP URLs computed by the
  * superclass constructor are never used because every public method is overridden.
  */
class MyndlaForPeersInProcessClient(producerCr: => no.ndla.myndlaapi.ComponentRegistry)(using
    props: BaseProps,
    ndlaClient: NdlaClient,
) extends MyNDLAApiClient {

  override def getUserWithFeideToken(feideToken: String): Try[MyNDLAUserDTO] = producerCr
    .userService
    .getMyNdlaUserDataDTO(Some(feideToken))

  override def getDomainUser(feideToken: String): Try[MyNDLAUser] = producerCr
    .userService
    .getMyNdlaUserDataDomain(Some(feideToken))

  override def getStatsFor(id: String, resourceTypes: List[ResourceType]): Try[List[SingleResourceStatsDTO]] =
    producerCr.folderReadService.getFavouriteStatsForResource(List(id), resourceTypes.map(_.toString))

  override def getMyNDLABundle: Try[MyNDLABundleDTO] = producerCr
    .folderReadService
    .getAllTheFavorites
    .map(MyNDLABundleDTO(_))
}
