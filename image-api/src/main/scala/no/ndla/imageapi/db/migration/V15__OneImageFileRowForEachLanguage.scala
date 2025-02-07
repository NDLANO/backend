/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V15__OneImageFileRowForEachLanguage extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
