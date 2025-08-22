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
import no.ndla.network.tapir.{Routes, SwaggerController, TapirApplication, TapirController, TapirHealthController}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.oembedproxy.controller.{OEmbedProxyController, SwaggerDocControllerConfig}
import no.ndla.oembedproxy.model.ErrorHandling
import no.ndla.oembedproxy.service.{OEmbedService, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties) extends TapirApplication[OEmbedProxyProperties] {
  given props: OEmbedProxyProperties                 = properties
  given routes: Routes                               = new Routes
  given errorHandling: ErrorHandling                 = new ErrorHandling
  given clock: Clock                                 = new Clock
  given ndlaClient: NdlaClient                       = new NdlaClient
  given providerService: ProviderService             = new ProviderService
  given oEmbedService: OEmbedService                 = new OEmbedService(None)
  given oEmbedProxyController: OEmbedProxyController = new OEmbedProxyController
  given healthController: TapirHealthController      = new TapirHealthController
  given myndlaApiClient: MyNDLAApiClient             = new MyNDLAApiClient

  given swagger: SwaggerController = new SwaggerController(
    List(
      oEmbedProxyController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  given services: List[TapirController] = swagger.getServices()
}
