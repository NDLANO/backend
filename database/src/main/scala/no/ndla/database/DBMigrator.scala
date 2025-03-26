/*
 * Part of NDLA database
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.migration.JavaMigration

trait DBMigrator {
  this: DataSource & HasDatabaseProps =>
  val migrator: DBMigrator

  case class DBMigrator(migrations: JavaMigration*) {
    def migrate(): Unit = {
      DataSource.connectToDatabase()

      val config = Flyway
        .configure()
        .javaMigrations(migrations*)
        .locations(props.MetaMigrationLocation)
        .dataSource(dataSource)
        .schemas(dataSource.getSchema)

      val withTable = props.MetaMigrationTable match {
        case Some(table) => config.table(table)
        case None        => config
      }

      val flyway = withTable.load()

      flyway.migrate(): Unit
    }
  }
}
