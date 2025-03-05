/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: FrontpageApiProperties) extends NdlaTapirMain[ComponentRegistry] {
  val componentRegistry = new ComponentRegistry(props)

  override def beforeStart(): Unit = {
    logger.info("Starting DB Migration")
    val dBstartMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done DB Migration took ${System.currentTimeMillis() - dBstartMillis} ms")
  }

  private def warmupRequest = (path: String) => Warmup.warmupRequest(props.ApplicationPort, path, Map.empty)
  override def warmup(): Unit = {
    warmupRequest("/frontpage-api/v1/frontpage")
    warmupRequest("/frontpage-api/v1/subjectpage/1")
    warmupRequest("/health")

    componentRegistry.healthController.setWarmedUp()
  }
}
