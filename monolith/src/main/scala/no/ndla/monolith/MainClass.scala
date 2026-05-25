/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: MonolithProperties) extends NdlaTapirMain[MonolithComponentRegistry] {
  val componentRegistry = new MonolithComponentRegistry(props)

  override def warmup(): Unit = {
    componentRegistry
      .warmupEndpoints
      .foreach { case (path, params) =>
        Warmup.warmupRequest(props.ApplicationPort, path, params)
      }
    componentRegistry.healthController.setRunning()
  }

  override def beforeStart(): Unit = {
    componentRegistry.runAllMigrations()
  }
}
