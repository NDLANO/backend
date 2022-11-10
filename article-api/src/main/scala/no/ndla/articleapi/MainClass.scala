/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import java.util
import javax.servlet.DispatcherType
import scala.io.Source

class MainClass(props: ArticleApiProperties) extends LazyLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")

    val startMillis = System.currentTimeMillis()

    val context = new ServletContextHandler()
    context.setContextPath("/")
    context.setInitParameter("org.scalatra.LifeCycle", "no.ndla.articleapi.ScalatraBootstrap")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
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
    context.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    // Necessary to mount ComponentRegistry members in ScalatraBootstrap
    context.setAttribute("ComponentRegistry", componentRegistry)

    val server = new Server(props.ApplicationPort)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${props.ApplicationPort} in $startTime ms.")

    server
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
