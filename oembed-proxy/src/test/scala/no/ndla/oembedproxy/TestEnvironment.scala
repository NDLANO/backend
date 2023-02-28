/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.{HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedService, ProviderService}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock

trait TestEnvironment {
  implicit val props: OEmbedProxyProperties = new OEmbedProxyProperties

  implicit val oEmbedService: OEmbedService                 = mock[OEmbedService]
  implicit val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  implicit val ndlaClient: NdlaClient                       = mock[NdlaClient]
  implicit val providerService: ProviderService             = mock[ProviderService]
  implicit val healthController: HealthController           = mock[HealthController]
  implicit val errorHelpers: ErrorHelpers                   = new ErrorHelpers

  def resetMocks(): Unit = {
    reset(oEmbedService, oEmbedProxyController, ndlaClient, providerService)
  }
}
