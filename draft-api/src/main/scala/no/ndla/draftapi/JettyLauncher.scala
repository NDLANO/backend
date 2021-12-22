/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import java.util

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.DispatcherType
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source
import scala.jdk.CollectionConverters._

object JettyLauncher extends LazyLogging {

  def startServer(port: Int): Server = {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    DBMigrator.migrate(ComponentRegistry.dataSource)
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")

    val startMillis = System.currentTimeMillis()

    buildMostUsedTagsCache()
    logger.info(s"Built tags cache in ${System.currentTimeMillis() - startMillis} ms.")

    val context = new ServletContextHandler()
    context.setContextPath("/")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, DraftApiProperties.ApplicationName)
    DraftApiProperties.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", DraftApiProperties.ApplicationName))
    }
    context.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(port)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port $port in $startTime ms.")

    server
  }

  private def buildMostUsedTagsCache(): Unit = {
    ComponentRegistry.readService.getTagUsageMap()
  }

  def main(args: Array[String]): Unit = {
    val envMap = System.getenv()
    envMap.asScala.foreach { case (k, v) => System.setProperty(k, v) }

    val server = startServer(DraftApiProperties.ApplicationPort)
    server.join()
  }
}
