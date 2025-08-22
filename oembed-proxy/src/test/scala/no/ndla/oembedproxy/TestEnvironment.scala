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
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{Routes, SwaggerController, TapirApplication, TapirHealthController}
import no.ndla.oembedproxy.controller.OEmbedProxyController
import no.ndla.oembedproxy.model.ErrorHandling
import no.ndla.oembedproxy.service.{OEmbedService, ProviderService}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[OEmbedProxyProperties] with MockitoSugar {
  implicit lazy val props: OEmbedProxyProperties                 = new OEmbedProxyProperties
  implicit lazy val errorHandling: ErrorHandling                 = new ErrorHandling(using props, clock)
  implicit lazy val routes: Routes                               = new Routes(using props, errorHandling, List.empty)
  implicit lazy val oEmbedService: OEmbedService                 = mock[OEmbedService]
  implicit lazy val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  implicit lazy val ndlaClient: NdlaClient                       = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient             = mock[MyNDLAApiClient]
  implicit lazy val providerService: ProviderService             = mock[ProviderService]
  implicit lazy val healthController: TapirHealthController      = mock[TapirHealthController]
  implicit lazy val clock: Clock                                 = mock[Clock]
  implicit lazy val swagger: SwaggerController                   = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
