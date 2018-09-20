/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import java.util

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.DispatcherType
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def main(args: Array[String]) {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    logger.info("Starting DB Migration")
    val dBstartMillis = System.currentTimeMillis()
    DBMigrator.migrate(ComponentRegistry.dataSource)
    logger.info(s"Done DB Migration tok ${System.currentTimeMillis() - dBstartMillis} ms")

    val startMillis = System.currentTimeMillis()
    val port = AudioApiProperties.ApplicationPort

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")

    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    servletContext.addServlet(classOf[ReportServlet], "/monitoring")
    servletContext.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, AudioApiProperties.ApplicationName)
    AudioApiProperties.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", AudioApiProperties.ApplicationName))
    }
    servletContext.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(port)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server.join()
  }
}
