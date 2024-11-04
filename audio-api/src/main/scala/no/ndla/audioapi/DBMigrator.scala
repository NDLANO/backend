/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.audioapi.db.migrationwithdependencies.{V5__AddAgreementToAudio, V6__TranslateUntranslatedAuthors}
import no.ndla.database.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

trait DBMigrator {
  this: Props with DataSource =>
  val migrator: DBMigrator

  class DBMigrator {
    def migrate(): MigrateResult = {
      val flyway = Flyway
        .configure()
        .javaMigrations(
          new V5__AddAgreementToAudio,
          new V6__TranslateUntranslatedAuthors
        )
        .locations("no/ndla/audioapi/db/migration")
        .table("schema_version") // Flyway's default table name changed, so we specify the old one.
        .dataSource(dataSource)
        // Seems like flyway uses datasource.getConnection().getScheme() which is null if the scheme does not exist.
        // Therefore we simply override it with dataSource.getScheme.
        // https://github.com/flyway/flyway/issues/2182
        .schemas(dataSource.getSchema)
        .load()
      flyway.migrate()
    }
  }
}
