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
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.{HealthController, OEmbedProxyController}
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties)
    extends BaseComponentRegistry[OEmbedProxyProperties]
    with OEmbedProxyController
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MemoizeHelpers
    with HealthController
    with Props
    with OEmbedProxyInfo
    with ErrorHelpers {
  override val props: OEmbedProxyProperties = properties
  implicit val swagger: OEmbedSwagger       = new OEmbedSwagger

  lazy val providerService       = new ProviderService
  lazy val oEmbedService         = new OEmbedService
  lazy val ndlaClient            = new NdlaClient
  lazy val oEmbedProxyController = new OEmbedProxyController
  lazy val resourcesApp          = new ResourcesApp
  lazy val healthController      = new HealthController
}
