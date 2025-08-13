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
  override lazy val props = new OEmbedProxyProperties

  lazy val oEmbedService: OEmbedService                 = mock[OEmbedService]
  lazy val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  lazy val ndlaClient: NdlaClient                       = mock[NdlaClient]
  lazy val myndlaApiClient: MyNDLAApiClient             = mock[MyNDLAApiClient]
  lazy val providerService: ProviderService             = mock[ProviderService]
  lazy val healthController: TapirHealthController      = mock[TapirHealthController]
  lazy val clock: SystemClock                           = mock[SystemClock]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
