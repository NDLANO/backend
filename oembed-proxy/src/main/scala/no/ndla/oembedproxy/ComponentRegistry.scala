/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.{OEmbedProxyController, SwaggerDocControllerConfig}
import no.ndla.oembedproxy.model.ErrorHandling
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}

class ComponentRegistry(properties: OEmbedProxyProperties)
    extends BaseComponentRegistry[OEmbedProxyProperties]
    with TapirApplication
    with OEmbedProxyController
    with OEmbedServiceComponent
    with NdlaClient
    with ProviderService
    with MemoizeHelpers
    with Props
    with ErrorHandling
    with Clock
    with SwaggerDocControllerConfig {
  override val props: OEmbedProxyProperties = properties

  lazy val providerService                         = new ProviderService
  lazy val oEmbedService                           = new OEmbedService
  lazy val ndlaClient                              = new NdlaClient
  lazy val oEmbedProxyController                   = new OEmbedProxyController
  lazy val healthController: TapirHealthController = new TapirHealthController
  lazy val myndlaApiClient: MyNDLAApiClient        = new MyNDLAApiClient

  lazy val clock = new SystemClock

  private val swagger = new SwaggerController(
    List(
      oEmbedProxyController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()

}
