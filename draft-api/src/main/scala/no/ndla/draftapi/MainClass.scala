/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.common.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: DraftApiProperties) extends LazyLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[DraftApiProperties, ComponentRegistry](
      "no.ndla.draftapi.ScalatraBootstrap",
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
