/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: MyNdlaApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest =
    (path: String, options: Map[String, String]) => Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setRunning()
  }

  override def beforeStart(): Unit = {
    componentRegistry.migrator.migrate()
  }
}
