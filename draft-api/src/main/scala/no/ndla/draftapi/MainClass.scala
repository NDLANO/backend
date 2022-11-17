/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: DraftApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[DraftApiProperties, ComponentRegistry](
      "no.ndla.draftapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting the db migration...")
        val startDBMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate()
        logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
      },
      warmupRequest => {
        warmupRequest("/draft-api/v1/drafts", Map("query" -> "norge", "fallback" -> "true"))
        warmupRequest("/draft-api/v1/drafts/1", Map.empty)
        warmupRequest("/health", Map.empty)
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
