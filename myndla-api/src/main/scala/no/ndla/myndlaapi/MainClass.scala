/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Warmup
import no.ndla.network.tapir.NdlaTapirMain
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

class MainClass(override val props: MyNdlaApiProperties) extends NdlaTapirMain[Eff] {
  val componentRegistry = new ComponentRegistry(props)

  private def warmupRequest = (path: String, options: Map[String, String]) =>
    Warmup.warmupRequest(props.ApplicationPort, path, options)

  override def warmup(): Unit = {
    warmupRequest("/health", Map.empty)

    componentRegistry.healthController.setWarmedUp()
  }

  def connectToDatabase(): Unit = {}

  override def beforeStart(): Unit = {
    logger.info("Starting the db migration...")
    val startDBMillis = System.currentTimeMillis()

    val dataSource: Option[HikariDataSource] =
      Option.when(props.migrateToLocalDB)(componentRegistry.DataSource.getHikariDataSource)
    val lpDs: HikariDataSource = componentRegistry.DataSource.getLpDs
    val ds = if (props.migrateToLocalDB) { dataSource.get }
    else { lpDs }

    ConnectionPool.singleton(new DataSourceConnectionPool(ds))

    if (props.migrateToLocalDB) {
      componentRegistry.migrator.migrate(dataSource.get)
      LpMigration(props, dataSource.get, lpDs).start()
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
