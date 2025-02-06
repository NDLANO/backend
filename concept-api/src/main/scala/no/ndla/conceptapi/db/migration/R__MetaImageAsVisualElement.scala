/*
 * Part of NDLA concept-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class R__MetaImageAsVisualElement extends BaseJavaMigration {
  override def getChecksum: Integer            = 0
  override def migrate(context: Context): Unit = {}
}
