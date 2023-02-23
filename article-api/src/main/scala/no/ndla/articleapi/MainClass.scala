/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: ArticleApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[ArticleApiProperties, ComponentRegistry](
      "no.ndla.articleapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting the db migration...")
        val startDBMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate()
        logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
      },
      warmupRequest => {
        warmupRequest("/article-api/v2/articles", Map("query" -> "norge", "fallback" -> "true"))
        warmupRequest("/article-api/v2/articles/1", Map("language" -> "nb"))
        warmupRequest("/article-api/v2/articles/ids/", Map("ids" -> "100,1000,2000,3000", "fallback" -> "true"))
        warmupRequest("/health", Map.empty)
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
