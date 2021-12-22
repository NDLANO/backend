/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.network.NdlaClient
import no.ndla.oembedproxy.controller.{HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}
import org.mockito.Mockito
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MockitoSugar
    with HealthController {
  val oEmbedService: OEmbedService = mock[OEmbedService]
  val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  val ndlaClient: NdlaClient = mock[NdlaClient]
  val providerService: ProviderService = mock[ProviderService]
  val healthController: HealthController = mock[HealthController]

  def resetMocks() = {
    Mockito.reset(oEmbedService, oEmbedProxyController, ndlaClient, providerService)
  }
}
