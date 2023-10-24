/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.learningpathapi.db.migrationwithdependencies.{
  V11__CreatedByNdlaStatusForOwnersWithRoles,
  V13__StoreNDLAStepsAsIframeTypes,
  V14__ConvertLanguageUnknown,
  V15__MergeDuplicateLanguageFields,
  V31__ArenaDefaultEnabledOrgs
}
import no.ndla.learningpathapi.integration.DataSource
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
          new V11__CreatedByNdlaStatusForOwnersWithRoles(props),
          new V13__StoreNDLAStepsAsIframeTypes(props),
          new V14__ConvertLanguageUnknown(props),
          new V15__MergeDuplicateLanguageFields(props),
          new V31__ArenaDefaultEnabledOrgs(props)
        )
        .locations("no/ndla/learningpathapi/db/migration")
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
