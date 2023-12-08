/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: OEmbedProxyProperties) extends NdlaTapirMain[Eff] {
  private val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path, params) => Warmup.warmupRequest(props.ApplicationPort, path, params)
  override def warmup(): Unit = {
    warmupRequest("/oembed-proxy/v1/oembed", Map("url" -> "https://ndla.no/article/1"))
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit =
    componentRegistry.providerService.loadProviders(): Unit

  override def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit =
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
}
