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
  implicit val props: OEmbedProxyProperties                 = new OEmbedProxyProperties
  implicit val errorHandler: ErrorHandling                  = new ErrorHandling
  implicit val routes: Routes                               = new Routes(using props, errorHandler, List.empty)
  implicit val oEmbedService: OEmbedService                 = mock[OEmbedService]
  implicit val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  implicit val ndlaClient: NdlaClient                       = mock[NdlaClient]
  implicit val myndlaApiClient: MyNDLAApiClient             = mock[MyNDLAApiClient]
  implicit val providerService: ProviderService             = mock[ProviderService]
  implicit val healthController: TapirHealthController      = mock[TapirHealthController]
  implicit val clock: Clock                                 = mock[Clock]
  implicit val swagger: SwaggerController                   = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
