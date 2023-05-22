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
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service, TapirHealthController}
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.OEmbedProxyController
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MockitoSugar
    with TapirHealthController
    with Props
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with MemoizeHelpers
    with ErrorHelpers
    with Clock
    with Service
    with NdlaMiddleware
    with Routes {
  override val props = new OEmbedProxyProperties

  val oEmbedService: OEmbedService                 = mock[OEmbedService]
  val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  val ndlaClient: NdlaClient                       = mock[NdlaClient]
  val providerService: ProviderService             = mock[ProviderService]
  val healthController: TapirHealthController      = mock[TapirHealthController]
  val clock: SystemClock                           = mock[SystemClock]

  def resetMocks(): Unit = {
    reset(oEmbedService, oEmbedProxyController, ndlaClient, providerService)
  }
}
