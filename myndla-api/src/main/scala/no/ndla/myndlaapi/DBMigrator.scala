/*
 * Part of NDLA myndla-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.database.DataSource
import org.flywaydb.core.Flyway

trait DBMigrator {
  this: Props with DataSource =>
  val migrator: DBMigrator

  class DBMigrator {

    def migrate(): Unit = {
      val flyway = Flyway
        .configure()
        .javaMigrations(
        )
        .locations("no/ndla/myndlaapi/db/migration")
        .dataSource(dataSource)
        // Seems like flyway uses datasource.getConnection().getScheme() which is null if the scheme does not exist.
        // Therefore we simply override it with dataSource.getScheme.
        // https://github.com/flyway/flyway/issues/2182
        .schemas(dataSource.getSchema)
        .load()
      flyway.migrate(): Unit
    }
  }

}
