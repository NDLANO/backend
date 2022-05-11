/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.network.NdlaClient
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.{CorrelationIdSupport, HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties)
    extends OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MemoizeHelpers
    with HealthController
    with Props
    with OEmbedProxyInfo
    with ErrorHelpers
    with CorrelationIdSupport {
  override val props: OEmbedProxyProperties = properties
  implicit val swagger: OEmbedSwagger       = new OEmbedSwagger

  lazy val providerService       = new ProviderService
  lazy val oEmbedService         = new OEmbedService
  lazy val ndlaClient            = new NdlaClient
  lazy val oEmbedProxyController = new OEmbedProxyController
  lazy val resourcesApp          = new ResourcesApp
  lazy val healthController      = new HealthController
}
