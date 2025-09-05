/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V33__ConvertLanguageUnknown extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
