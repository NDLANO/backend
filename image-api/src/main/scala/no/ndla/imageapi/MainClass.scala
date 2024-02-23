/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: ImageApiProperties) extends NdlaTapirMain[Eff] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/image-api/v2/images", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/image-api/v2/images/1", Map("language" -> "nb"))
    warmupRequest("/image-api/raw/id/1", Map.empty)
    warmupRequest("/health", Map.empty)
  }

  override def beforeStart(): Unit = {
    logger.info("Starting DB Migration")
    val DbStartMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate(): Unit
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - DbStartMillis} ms")

    componentRegistry.imageSearchService.createEmptyIndexIfNoIndexesExist()
    componentRegistry.tagSearchService.createEmptyIndexIfNoIndexesExist()
  }

  override def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit =
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
}
