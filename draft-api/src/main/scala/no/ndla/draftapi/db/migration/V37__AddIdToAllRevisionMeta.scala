/*
 * Part of NDLA draft-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V37__AddIdToAllRevisionMeta extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
