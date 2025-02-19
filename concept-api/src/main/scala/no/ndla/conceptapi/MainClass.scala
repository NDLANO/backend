/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: ConceptApiProperties) extends NdlaTapirMain {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/concept-api/v1/concepts", Map("query" -> "norge", "fallback" -> "true"))
    warmupRequest("/concept-api/v1/concepts/1", Map.empty)
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit = {
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()
    componentRegistry.migrator.migrate()
    logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
  }

  override def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit =
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
}
