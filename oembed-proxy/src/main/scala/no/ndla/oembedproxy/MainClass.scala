/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.Environment.setPropsFromEnv
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

class MainClass(props: OEmbedProxyProperties) extends LazyLogging {
  val componentRegistry = new ComponentRegistry(props)

  private def fetchProviderList() = {
    componentRegistry.providerService.loadProviders()
  }

  def startServer(): Server = {
    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString
    )

    val startMillis = System.currentTimeMillis
    val port        = props.ApplicationPort

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")
    servletContext.setInitParameter("org.scalatra.LifeCycle", "no.ndla.oembedproxy.ScalatraBootstrap")
    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    // Necessary to mount ComponentRegistry members in ScalatraBootstrap
    servletContext.setAttribute("ComponentRegistry", componentRegistry)

    fetchProviderList()

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
