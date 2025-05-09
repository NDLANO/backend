/*
 * Part of NDLA article-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class R__RemoveDummyMetaDescription extends BaseJavaMigration {
  override def getChecksum: Integer            = 1
  override def migrate(context: Context): Unit = {}
}
