/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: LearningpathApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[LearningpathApiProperties, ComponentRegistry](
      "no.ndla.learningpathapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting the db migration...")
        val startDBMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate(): Unit
        logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
      },
      warmupRequest => {
        warmupRequest("/learningpath-api/v2/learningpaths", Map("query" -> "norge", "fallback" -> "true"))
        warmupRequest("/learningpath-api/v2/learningpaths/1", Map.empty)
        warmupRequest("/health", Map.empty)
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
