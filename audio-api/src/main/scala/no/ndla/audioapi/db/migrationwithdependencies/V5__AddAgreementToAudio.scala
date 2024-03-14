/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migrationwithdependencies

import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V5__AddAgreementToAudio extends BaseJavaMigration with StrictLogging {
  override def migrate(context: Context): Unit = {}
}
