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
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import org.http4s.{Request, Response}

class MainClass(override val props: OEmbedProxyProperties) extends NdlaTapirMain {
  private val componentRegistry                            = new ComponentRegistry(props)
  override val app: Kleisli[IO, Request[IO], Response[IO]] = componentRegistry.routes

  private def warmupRequest = (path, params) => Warmup.warmupRequest(props.ApplicationPort, path, params)
  override def warmup(): Unit = {
    warmupRequest("/oembed-proxy/v1/oembed", Map("url" -> "https://ndla.no/article/1"))
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit =
    componentRegistry.providerService.loadProviders(): Unit
}
