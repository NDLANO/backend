/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migrationwithdependencies

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V6__AddAgreementToImages extends BaseJavaMigration with StrictLogging {
  override def migrate(context: Context): Unit = {}
}
