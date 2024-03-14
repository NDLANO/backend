/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.db.migrationwithdependencies

import no.ndla.draftapi.DraftApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}

class V20__UpdateH5PDomainForFF(props: DraftApiProperties) extends BaseJavaMigration {
  override def migrate(context: Context): Unit = {}
}
