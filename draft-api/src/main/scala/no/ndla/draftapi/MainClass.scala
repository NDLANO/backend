/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: DraftApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest =
    (path: String, options: Map[String, String]) => Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/draft-api/v1/drafts", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/draft-api/v1/drafts/1", Map.empty)
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setRunning()
  }

  override def beforeStart(): Unit = {
    componentRegistry.migrator.migrate()
  }
}
