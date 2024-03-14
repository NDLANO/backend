/*
 * Part of NDLA concept-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V20__DeleteUnintentionalNullValues extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
