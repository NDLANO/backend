/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import cats.data.Kleisli
import cats.effect.IO
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.network.NdlaClient
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.network.tapir.{NdlaMiddleware, Routes, Service}
import no.ndla.oembedproxy.caching.MemoizeHelpers
import no.ndla.oembedproxy.controller.{HealthController, OEmbedProxyController, SwaggerDocControllerConfig}
import no.ndla.oembedproxy.model.ErrorHelpers
import no.ndla.oembedproxy.service.{OEmbedServiceComponent, ProviderService}
import org.http4s.{Request, Response}

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
    with ErrorHelpers
    with Routes
    with Clock
    with Service
    with NdlaMiddleware
    with SwaggerDocControllerConfig {
  override val props: OEmbedProxyProperties = properties

  lazy val providerService       = new ProviderService
  lazy val oEmbedService         = new OEmbedService
  lazy val ndlaClient            = new NdlaClient
  lazy val oEmbedProxyController = new OEmbedProxyController
  lazy val healthController      = new HealthController

  lazy val clock = new SystemClock

  private val services: List[Service] = List(
    oEmbedProxyController,
    healthController
  )

  private val swaggerDocController = new SwaggerController(services, SwaggerDocControllerConfig.swaggerInfo)

  def routes: Kleisli[IO, Request[IO], Response[IO]] = Routes.build(services :+ swaggerDocController)
}
