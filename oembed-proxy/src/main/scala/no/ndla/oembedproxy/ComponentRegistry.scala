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
  TapirApplication,
  TapirController,
  TapirHealthController,
}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.oembedproxy.controller.{ControllerErrorHandling, OEmbedProxyController, SwaggerDocControllerConfig}
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

  given swagger: SwaggerController =
    new SwaggerController(List(oEmbedProxyController, healthController), SwaggerDocControllerConfig.swaggerInfo)

  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
