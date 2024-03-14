/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migrationwithdependencies

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V15__MergeDuplicateLanguageFields extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
