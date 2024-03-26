/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.db.migrationwithdependencies

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V11__CreatedByNdlaStatusForOwnersWithRoles extends BaseJavaMigration with StrictLogging {
  override def migrate(context: Context): Unit = {}
}
