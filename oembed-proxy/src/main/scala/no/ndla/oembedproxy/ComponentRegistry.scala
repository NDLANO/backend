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
  override val props: OEmbedProxyProperties                 = properties
  implicit val p: OEmbedProxyProperties                     = props
  implicit val swagger: OEmbedSwagger                       = new OEmbedSwagger()
  implicit val errorHelpers: ErrorHelpers                   = new ErrorHelpers
  implicit val providerService: ProviderService             = new ProviderService
  implicit val oEmbedService: OEmbedService                 = new OEmbedService
  implicit val ndlaClient: NdlaClient                       = new NdlaClient
  implicit val oEmbedProxyController: OEmbedProxyController = new OEmbedProxyController
  implicit val resourcesApp: ResourcesApp                   = new ResourcesApp
  override val healthController: HealthController           = new HealthController
  implicit val h: HealthController                          = healthController
}
