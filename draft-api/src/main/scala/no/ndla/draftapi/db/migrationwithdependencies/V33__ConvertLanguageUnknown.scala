/*
 * Part of NDLA draft-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.draftapi.{DraftApiProperties, Props}
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V33__ConvertLanguageUnknown(properties: DraftApiProperties) extends BaseJavaMigration with Props {
  override val props: DraftApiProperties       = properties
  override def migrate(context: Context): Unit = {}
}
