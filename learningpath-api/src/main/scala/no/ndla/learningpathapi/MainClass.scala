/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: LearningpathApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/learningpath-api/v2/learningpaths", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/learningpath-api/v2/learningpaths/1", Map.empty)
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit = {
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
  }
}
