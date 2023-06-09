/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.network.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: ConceptApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[ConceptApiProperties, ComponentRegistry](
      "no.ndla.conceptapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting the db migration...")
        val startDBMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate(): Unit
        logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
      },
      warmupRequest => {
        warmupRequest("/concept-api/v1/concepts", Map("query" -> "norge", "fallback" -> "true"))
        warmupRequest("/concept-api/v1/concepts/1", Map.empty)
        warmupRequest("/health", Map.empty)
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
