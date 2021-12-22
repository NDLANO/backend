/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def fetchProviderList = {
    ComponentRegistry.providerService.loadProviders()
  }

  def main(args: Array[String]): Unit = {
    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString)

    val startMillis = System.currentTimeMillis
    val port = OEmbedProxyProperties.ApplicationPort

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")
    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    fetchProviderList

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server.join()
  }
}
