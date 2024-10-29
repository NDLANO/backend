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
    with OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MockitoSugar
    with Props
    with MemoizeHelpers
    with ErrorHandling
    with Clock {
  override val props = new OEmbedProxyProperties

  val oEmbedService: OEmbedService                 = mock[OEmbedService]
  val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  val ndlaClient: NdlaClient                       = mock[NdlaClient]
  val myndlaApiClient: MyNDLAApiClient             = mock[MyNDLAApiClient]
  val providerService: ProviderService             = mock[ProviderService]
  val healthController: TapirHealthController      = mock[TapirHealthController]
  val clock: SystemClock                           = mock[SystemClock]

  def services: List[TapirController] = List.empty

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
