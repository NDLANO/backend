/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V10__RemoveImageVisualElementsWithoutIds extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
