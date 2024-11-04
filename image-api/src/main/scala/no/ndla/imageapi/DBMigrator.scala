/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.database.DataSource
import no.ndla.imageapi.db.migrationwithdependencies.{V6__AddAgreementToImages, V7__TranslateUntranslatedAuthors}
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

trait DBMigrator {
  this: Props with DataSource =>
  val migrator: DBMigrator

  class DBMigrator {
    def migrate(): MigrateResult = {
      val flyway = Flyway
        .configure()
        .table("schema_version")
        .javaMigrations(
          new V6__AddAgreementToImages,
          new V7__TranslateUntranslatedAuthors
        )
        .locations("no/ndla/imageapi/db/migration")
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
