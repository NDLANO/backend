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
import no.ndla.network.tapir.{AllErrors, ErrorHelpers, Routes, SwaggerController, TapirApplication, TapirController, TapirErrorHandling, TapirHealthController}
import no.ndla.oembedproxy.controller.OEmbedProxyController
import no.ndla.oembedproxy.service.{OEmbedService, ProviderService}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[OEmbedProxyProperties] with MockitoSugar {
  implicit lazy val props: OEmbedProxyProperties                 = new OEmbedProxyProperties
  implicit lazy val clock: Clock                                 = mock[Clock]
  implicit lazy val errorHelpers: ErrorHelpers                  = new ErrorHelpers
  implicit lazy val errorHandling: TapirErrorHandling           = new TapirErrorHandling {
    override def handleErrors: PartialFunction[Throwable, AllErrors] = { case e: Throwable =>
      errorHelpers.generic
    }
  }
  implicit lazy val services: List[TapirController] = List.empty
  implicit lazy val routes: Routes                               = new Routes
  implicit lazy val oEmbedService: OEmbedService                 = mock[OEmbedService]
  implicit lazy val oEmbedProxyController: OEmbedProxyController = mock[OEmbedProxyController]
  implicit lazy val ndlaClient: NdlaClient                       = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient             = mock[MyNDLAApiClient]
  implicit lazy val providerService: ProviderService             = mock[ProviderService]
  implicit lazy val healthController: TapirHealthController      = mock[TapirHealthController]
  implicit lazy val swagger: SwaggerController                   = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(oEmbedService)
    reset(oEmbedProxyController)
    reset(ndlaClient)
    reset(providerService)
  }
}
