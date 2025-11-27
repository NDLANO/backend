/*
 * Part of NDLA database
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.database

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

/** Helper class to mark a migration as already executed in the environments. Useful to allow deletion of unmaintained
  * code in a migration that will never be ran again
  */
class FinishedMigration extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
