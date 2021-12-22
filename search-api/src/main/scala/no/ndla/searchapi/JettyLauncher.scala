/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.LazyLogging
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import no.ndla.searchapi.service.StandaloneIndexing
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import java.util
import javax.servlet.DispatcherType
import scala.io.Source
import scala.jdk.CollectionConverters.MapHasAsScala

object JettyLauncher extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    val envMap = System.getenv()
    envMap.asScala.foreach { case (k, v) => System.setProperty(k, v) }

    if (SearchApiProperties.booleanOrFalse("STANDALONE_INDEXING_ENABLED")) {
      StandaloneIndexing.doStandaloneIndexing()
    } else {
      val server = startServer(SearchApiProperties.ApplicationPort)
      server.join()
    }
  }

  def startServer(port: Int): Server = {
    val startMillis = System.currentTimeMillis()

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, SearchApiProperties.ApplicationName)
    SearchApiProperties.Environment match {
      case "local" =>
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", SearchApiProperties.ApplicationName))
    }
    context.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(port)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server
  }

}
