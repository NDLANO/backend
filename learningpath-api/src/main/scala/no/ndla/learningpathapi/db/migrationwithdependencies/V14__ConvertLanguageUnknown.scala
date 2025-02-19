/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migrationwithdependencies

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V14__ConvertLanguageUnknown extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
