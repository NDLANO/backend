/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V25__RemoveDomainFromH5PUrl extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
