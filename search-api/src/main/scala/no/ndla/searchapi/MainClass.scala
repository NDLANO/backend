/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.common.Environment.booleanPropOrFalse
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import no.ndla.searchapi.service.StandaloneIndexing
import sttp.tapir.server.netty.sync.NettySyncServerBinding

class MainClass(override val props: SearchApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest =
    (path: String, options: Map[String, String]) => Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/search-api/v1/search", Map("query" -> "norge"))
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit = {}

  override def startServerAndWait(name: String, port: Int)(onStartup: NettySyncServerBinding => Unit): Unit = {
    if (booleanPropOrFalse("STANDALONE_INDEXING_ENABLED")) {
      new StandaloneIndexing(props, componentRegistry).doStandaloneIndexing()
    } else {
      super.startServerAndWait(name, port)(onStartup)
    }
  }
}
