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
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  SwaggerInfo,
  TapirApplication,
  TapirController,
  TapirHealthController,
}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.oembedproxy.controller.{ControllerErrorHandling, OEmbedProxyController}
import no.ndla.oembedproxy.service.{OEmbedService, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties) extends TapirApplication[OEmbedProxyProperties] {
  given props: OEmbedProxyProperties                 = properties
  given clock: Clock                                 = new Clock
  given errorHelpers: ErrorHelpers                   = new ErrorHelpers
  given errorHandling: ErrorHandling                 = new ControllerErrorHandling
  given ndlaClient: NdlaClient                       = new NdlaClient
  given myndlaApiClient: MyNDLAApiClient             = new MyNDLAApiClient
  given providerService: ProviderService             = new ProviderService
  given oEmbedService: OEmbedService                 = new OEmbedService(None)
  given healthController: TapirHealthController      = new TapirHealthController
  given oEmbedProxyController: OEmbedProxyController = new OEmbedProxyController

  given swaggerInfo: SwaggerInfo =
    SwaggerInfo(prefix = "oembed-proxy", description = "Convert any NDLA resource to an oEmbed embeddable resource.")
  given swagger: SwaggerController = new SwaggerController(healthController, oEmbedProxyController)

  given services: List[TapirController] = swagger.allServices
  given routes: Routes                  = new Routes
}
