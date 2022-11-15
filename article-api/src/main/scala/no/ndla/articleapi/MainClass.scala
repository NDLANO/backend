/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: ArticleApiProperties) extends LazyLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[ArticleApiProperties, ComponentRegistry](
      "no.ndla.articleapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting the db migration...")
        val startDBMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate()
        logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
