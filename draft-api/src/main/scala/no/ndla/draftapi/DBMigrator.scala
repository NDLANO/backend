/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import draftapi.db.migrationwithdependencies.{
  R__RemoveEmptyStringLanguageFields,
  R__RemoveStatusPublishedArticles,
  R__SetArticleLanguageFromTaxonomy,
  V20__UpdateH5PDomainForFF,
  V23__UpdateH5PDomainForFFVisualElement,
  V33__ConvertLanguageUnknown
}
import no.ndla.draftapi.integration.DataSource
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
          new R__RemoveEmptyStringLanguageFields(props),
          new R__RemoveStatusPublishedArticles(props),
          new R__SetArticleLanguageFromTaxonomy(props),
          new R__SetArticleLanguageFromTaxonomy(props),
          new V20__UpdateH5PDomainForFF(props),
          new V23__UpdateH5PDomainForFFVisualElement(props),
          new V33__ConvertLanguageUnknown(props)
        )
        .locations("draftapi/db/migration")
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
