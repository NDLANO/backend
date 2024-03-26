/*
 * Part of NDLA article-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migrationwithdependencies

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class R__SetArticleTypeFromTaxonomy extends BaseJavaMigration {
  override def getChecksum: Integer            = 0
  override def migrate(context: Context): Unit = {}
}
