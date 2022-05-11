/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.network.NdlaClient
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.{CorrelationIdSupport, HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MockitoSugar
    with HealthController
    with Props
    with CorrelationIdSupport
    with MemoizeHelpers
    with ErrorHelpers
    with OEmbedProxyInfo {
  override val props = new OEmbedProxyProperties

  val oEmbedService: OEmbedService                 = mock[OEmbedService]
  val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  val ndlaClient: NdlaClient                       = mock[NdlaClient]
  val providerService: ProviderService             = mock[ProviderService]
  val healthController: HealthController           = mock[HealthController]

  def resetMocks(): Unit = {
    reset(oEmbedService, oEmbedProxyController, ndlaClient, providerService)
  }
}
