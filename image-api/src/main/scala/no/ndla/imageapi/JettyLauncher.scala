/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import no.ndla.common.Environment.setPropsFromEnv
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import java.util
import javax.servlet.DispatcherType
import scala.io.Source

object JettyLauncher extends LazyLogging {

  def startServer(port: Int): Server = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    logger.info("Starting DB Migration")
    val DbStartMillis = System.currentTimeMillis()
    DBMigrator.migrate(ComponentRegistry.dataSource)
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - DbStartMillis} ms")

    val startMillis = System.currentTimeMillis()

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")

    ComponentRegistry.imageSearchService.createEmptyIndexIfNoIndexesExist()
    ComponentRegistry.tagSearchService.createEmptyIndexIfNoIndexesExist()

    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    servletContext.setInitParameter("org.scalatra.cors.allowCredentials", "false")

    servletContext.addServlet(classOf[ReportServlet], "/monitoring")
    servletContext.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, ImageApiProperties.ApplicationName)
    ImageApiProperties.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", ImageApiProperties.ApplicationName))
    }
    servletContext.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")
    server
  }

  def main(args: Array[String]): Unit = {
    setPropsFromEnv()

    val server = startServer(ImageApiProperties.ApplicationPort)
    server.join()
  }
}
