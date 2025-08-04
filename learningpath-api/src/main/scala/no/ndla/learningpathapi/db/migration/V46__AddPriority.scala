/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.{Json, parser}
import no.ndla.database.DocumentMigration
import no.ndla.common.model.domain.Priority

class V46__AddPriority extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "learningpaths"

  override def convertColumn(document: String): String = {
    val oldDocument = parser.parse(document).toTry.get
    oldDocument.mapObject(_.add("priority", Json.fromString(Priority.Unspecified.entryName))).noSpaces
  }

}
