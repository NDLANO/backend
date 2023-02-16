/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: OEmbedProxyProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  private def fetchProviderList() = {
    componentRegistry.providerService.loadProviders()
  }

  def startServer(): Server = {
    new NdlaScalatraServer[OEmbedProxyProperties, ComponentRegistry](
      "no.ndla.oembedproxy.ScalatraBootstrap",
      componentRegistry, {
        fetchProviderList()
      },
      warmupRequest => {
        warmupRequest("/oembed-proxy/v1/oembed", Map("url" -> "https://ndla.no/article/1"))
        warmupRequest("/health", Map.empty)
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
