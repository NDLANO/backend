/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.OEmbedProxyController
import no.ndla.oembedproxy.model.ErrorHandling
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with MockitoSugar {
  given props = new OEmbedProxyProperties

  given oEmbedService: OEmbedService                 = mock[OEmbedService]
  given oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  given ndlaClient: NdlaClient                       = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient             = mock[MyNDLAApiClient]
  given providerService: ProviderService             = mock[ProviderService]
  given healthController: TapirHealthController      = mock[TapirHealthController]
  given clock: SystemClock                           = mock[SystemClock]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
