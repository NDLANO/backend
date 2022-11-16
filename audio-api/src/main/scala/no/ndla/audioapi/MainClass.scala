/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.scalatra.NdlaScalatraServer
import org.eclipse.jetty.server.Server

class MainClass(props: AudioApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[AudioApiProperties, ComponentRegistry](
      "no.ndla.audioapi.ScalatraBootstrap",
      componentRegistry, {
        logger.info("Starting DB Migration")
        val dBstartMillis = System.currentTimeMillis()
        componentRegistry.migrator.migrate()
        logger.info(s"Done DB Migration took ${System.currentTimeMillis() - dBstartMillis} ms")
      }
    )
  }

  def start(): Unit = {
    val server = startServer()
    server.join()
  }
}
