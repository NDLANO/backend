/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.common.Environment.booleanPropOrFalse
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import no.ndla.imageapi.service.StandaloneVariantGeneration
import sttp.tapir.server.netty.sync.NettySyncServerBinding

class MainClass(override val props: ImageApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest =
    (path: String, options: Map[String, String]) => Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/image-api/v2/images", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/image-api/v2/images/1", Map("language" -> "nb"))
    warmupRequest("/image-api/raw/id/1", Map.empty)
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setRunning()
  }

  override def beforeStart(): Unit = {
    componentRegistry.migrator.migrate()

    if (!props.StandaloneVariantGenerationEnabled) {
      componentRegistry.imageSearchService.createEmptyIndexIfNoIndexesExist()
      componentRegistry.tagSearchService.createEmptyIndexIfNoIndexesExist()
    }
  }

  override def startServerAndWait(name: String, port: Int)(onStartup: NettySyncServerBinding => Unit): Unit = {
    if (booleanPropOrFalse("STANDALONE_VARIANT_GENERATION_ENABLED")) {
      StandaloneVariantGeneration(
        componentRegistry.writeService,
        componentRegistry.imageStorage,
        componentRegistry.imageRepository,
        componentRegistry.dbUtility,
        props,
      ).doStandaloneVariantGeneration()
    } else {
      super.startServerAndWait(name, port)(onStartup)
    }
  }
}
