/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(val props: AudioApiProperties) extends NdlaTapirMain[Eff] {
  private val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path, params) => Warmup.warmupRequest(props.ApplicationPort, path, params)
  def warmup(): Unit = {
    warmupRequest("/audio-api/v1/audio", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/audio-api/v1/audio/1", Map("language" -> "nb"))
    warmupRequest("/audio-api/v1/series", Map("language" -> "nb"))
    warmupRequest("/audio-api/v1/series/1", Map("language" -> "nb"))
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  def beforeStart(): Unit = {
    logger.info("Starting DB Migration")
    val dBstartMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate(): Unit
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - dBstartMillis} ms")
  }

  override def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit =
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
}
