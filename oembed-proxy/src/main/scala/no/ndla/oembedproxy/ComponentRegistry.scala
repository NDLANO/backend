/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.oembedproxy.controller.{HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedService, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties) extends BaseComponentRegistry[OEmbedProxyProperties] {

  implicit val ndlaClient: NdlaClient = new NdlaClient

  override implicit val props: OEmbedProxyProperties        = properties
  implicit val providerService: ProviderService             = new ProviderService
  implicit val swagger: OEmbedSwagger                       = new OEmbedSwagger()
  implicit val errorHelpers: ErrorHelpers                   = new ErrorHelpers
  implicit val oEmbedService: OEmbedService                 = new OEmbedService(None)
  implicit val oEmbedProxyController: OEmbedProxyController = new OEmbedProxyController
  implicit val resourcesApp: ResourcesApp                   = new ResourcesApp
  override implicit val healthController: HealthController  = new HealthController
}
