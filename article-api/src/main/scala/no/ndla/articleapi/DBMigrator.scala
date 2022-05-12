/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import articleapi.db.migrationwithdependencies.{R__SetArticleLanguageFromTaxonomy, V8__CopyrightFormatUpdated}
import no.ndla.articleapi.integration.DataSource
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
          new R__SetArticleLanguageFromTaxonomy(props),
          new R__SetArticleLanguageFromTaxonomy(props),
          new V8__CopyrightFormatUpdated(props)
        )
        .locations("articleapi/db/migration")
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
