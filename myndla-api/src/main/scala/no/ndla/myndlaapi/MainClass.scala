/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi

import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain

class MainClass(override val props: MyNdlaApiProperties) extends NdlaTapirMain[Eff] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  override def beforeStart(): Unit = {
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()

    componentRegistry.DataSource.connectToDatabase()

    if (props.migrateToLocalDB) {
      componentRegistry.migrator.migrate(componentRegistry.dataSource.get)
      LpMigration(props, componentRegistry.dataSource.get, componentRegistry.lpDs).start()
      logger.info(s"Done db migration, took ${System.currentTimeMillis() - startDBMillis}ms")
    } else {
      logger.info(
        "Skipping db migration, because we're running against learningpath-api db for now... use `LP_MIGRATE=true` to use local db."
      )
    }
  }

  override def startServer(name: String, port: Int)(warmupFunc: => Unit): Unit = {
    componentRegistry.Routes.startJdkServer(name, port)(warmupFunc)
  }
}
