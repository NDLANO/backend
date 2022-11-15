/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: OEmbedProxyProperties) extends LazyLogging {
  val componentRegistry = new ComponentRegistry(props)

  private def fetchProviderList() = {
    componentRegistry.providerService.loadProviders()
  }

  def startServer(): Server = {
    new NdlaScalatraServer[OEmbedProxyProperties, ComponentRegistry](
      "no.ndla.oembedproxy.ScalatraBootstrap",
      componentRegistry, {
        fetchProviderList()
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
