/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V2__RemoveFullFromImagePath extends BaseJavaMigration with StrictLogging {
  override def migrate(context: Context): Unit = {}
}
