/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Environment.booleanPropOrFalse
import no.ndla.common.scalatra.NdlaScalatraServer
import no.ndla.searchapi.service.StandaloneIndexing
import org.eclipse.jetty.server.Server

class MainClass(props: SearchApiProperties) extends StrictLogging {
  val componentRegistry = new ComponentRegistry(props)

  def startServer(): Server = {
    new NdlaScalatraServer[SearchApiProperties, ComponentRegistry](
      "no.ndla.searchapi.ScalatraBootstrap",
      componentRegistry
    )
  }

  def start(): Unit = {
    if (booleanPropOrFalse("STANDALONE_INDEXING_ENABLED")) {
      new StandaloneIndexing(props, componentRegistry).doStandaloneIndexing()
    } else {
      val server = startServer()
      server.join()
    }
  }
}
