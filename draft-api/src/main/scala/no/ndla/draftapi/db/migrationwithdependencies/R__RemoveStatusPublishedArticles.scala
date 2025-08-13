/*
 * Part of NDLA draft-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.draftapi.{DraftApiProperties, Props}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class R__RemoveStatusPublishedArticles(properties: DraftApiProperties) extends BaseJavaMigration with Props {
  override lazy val props: DraftApiProperties  = properties
  override def getChecksum: Integer            = 0
  override def migrate(context: Context): Unit = {}
}
