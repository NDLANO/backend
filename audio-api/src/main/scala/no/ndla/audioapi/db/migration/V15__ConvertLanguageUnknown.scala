/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V15__ConvertLanguageUnknown extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
