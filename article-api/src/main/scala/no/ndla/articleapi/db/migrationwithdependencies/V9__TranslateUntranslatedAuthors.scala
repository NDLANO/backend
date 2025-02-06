/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migrationwithdependencies

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V9__TranslateUntranslatedAuthors extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
