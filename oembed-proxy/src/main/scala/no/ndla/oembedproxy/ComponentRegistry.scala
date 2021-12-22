/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.network.NdlaClient
import no.ndla.oembedproxy.controller.{HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}

object ComponentRegistry
    extends OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with HealthController {
  implicit val swagger: OEmbedSwagger = new OEmbedSwagger

  lazy val providerService = new ProviderService
  lazy val oEmbedService = new OEmbedService
  lazy val ndlaClient = new NdlaClient
  lazy val oEmbedProxyController = new OEmbedProxyController
  lazy val resourcesApp = new ResourcesApp
  lazy val healthController = new HealthController
}
