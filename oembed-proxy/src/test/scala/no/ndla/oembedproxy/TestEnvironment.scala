/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.common.Clock
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, TapirHealthController}
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.OEmbedProxyController
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MockitoSugar
    with TapirHealthController
    with Props
    with MemoizeHelpers
    with ErrorHelpers
    with Clock
    with NdlaMiddleware
    with Routes[Eff] {
  override val props = new OEmbedProxyProperties

  val oEmbedService: OEmbedService                 = mock[OEmbedService]
  val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  val ndlaClient: NdlaClient                       = mock[NdlaClient]
  val providerService: ProviderService             = mock[ProviderService]
  val healthController: TapirHealthController[Eff] = mock[TapirHealthController[Eff]]
  val clock: SystemClock                           = mock[SystemClock]

  def services: List[Service[Eff]] = List.empty

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
