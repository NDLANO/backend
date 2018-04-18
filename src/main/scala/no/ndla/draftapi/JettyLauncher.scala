/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def buildMostUsedTagsCache = {
    ComponentRegistry.readService.getTagUsageMap()
  }

  def main(args: Array[String]) {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    DBMigrator.migrate(ComponentRegistry.dataSource)
    logger.info(s"Done db migration, tok ${System.currentTimeMillis() - startDBMillis}ms")

    val startMillis = System.currentTimeMillis()

    buildMostUsedTagsCache
    logger.info(s"Built tags cache in ${System.currentTimeMillis() - startMillis} ms.")

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    val server = new Server(DraftApiProperties.ApplicationPort)
    server.setHandler(context)
    server.start

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${DraftApiProperties.ApplicationPort} in $startTime ms.")

    server.join
  }
}
