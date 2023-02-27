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
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties) extends BaseComponentRegistry[OEmbedProxyProperties] {
  
  given props: OEmbedProxyProperties = properties

  given swagger: OEmbedSwagger = new OEmbedSwagger

  given providerService: ProviderService       = new ProviderService

  given oEmbedService: OEmbedService          = new OEmbedService

  given ndlaClient: NdlaClient             = new NdlaClient

  given oEmbedProxyController: OembedProxyController  = new OEmbedProxyController

  given resourcesApp: ResourcesApp           = new ResourcesApp

  given healthController: HealthController       = new HealthController

}
