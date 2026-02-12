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

class MainClass(val props: AudioApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path, params) => Warmup.warmupRequest(props.ApplicationPort, path, params)
  def warmup(): Unit        = {
    warmupRequest("/audio-api/v1/audio", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/audio-api/v1/audio/1", Map("language" -> "nb"))
    warmupRequest("/audio-api/v1/series", Map("language" -> "nb"))
    warmupRequest("/audio-api/v1/series/1", Map("language" -> "nb"))
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setRunning()
  }

  def beforeStart(): Unit = {
    componentRegistry.migrator.migrate()
  }
}
