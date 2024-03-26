/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migration

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V31__ConvertBrightcoveIds extends BaseJavaMigration with StrictLogging {
  override def migrate(context: Context): Unit = {}
}
