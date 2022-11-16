/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: ImageApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[ImageApiProperties, ComponentRegistry](
      "no.ndla.imageapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting DB Migration")
        val DbStartMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate()
        logger.info(s"Done DB Migration took ${System.currentTimeMillis() - DbStartMillis} ms")

        componentRegistry.imageSearchService.createEmptyIndexIfNoIndexesExist()
        componentRegistry.tagSearchService.createEmptyIndexIfNoIndexesExist()
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
