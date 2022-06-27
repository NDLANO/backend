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
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import java.util
import javax.servlet.DispatcherType
import scala.io.Source

class MainClass(props: ImageApiProperties) extends LazyLogging {
  private val initTime  = System.currentTimeMillis()
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    logger.info("Starting DB Migration")
    val DbStartMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - DbStartMillis} ms")

    val startMillis = System.currentTimeMillis()

    componentRegistry.imageSearchService.createEmptyIndexIfNoIndexesExist()
    componentRegistry.tagSearchService.createEmptyIndexIfNoIndexesExist()

    val servletContext = new ServletContextHandler
    servletContext.setContextPath("/")
    servletContext.setInitParameter("org.scalatra.LifeCycle", "no.ndla.imageapi.ScalatraBootstrap")
    servletContext.addEventListener(new ScalatraListener)
    servletContext.addServlet(classOf[DefaultServlet], "/")
    servletContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    servletContext.setInitParameter("org.scalatra.cors.allowCredentials", "false")

    servletContext.addServlet(classOf[ReportServlet], "/monitoring")
    servletContext.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, props.ApplicationName)
    props.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(
          Parameter.CLOUDWATCH_NAMESPACE.getCode,
          "NDLA/APP".replace("APP", props.ApplicationName)
        )
    }
    servletContext.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    // Necessary to mount ComponentRegistry members in ScalatraBootstrap
    servletContext.setAttribute("ComponentRegistry", componentRegistry)

    val server = new Server(props.ApplicationPort)
    server.setHandler(servletContext)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${props.ApplicationPort} in $startTime ms.")
    logger.info(s"Entire ${props.ApplicationName} started in ${System.currentTimeMillis() - initTime} ms.")
    server
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
