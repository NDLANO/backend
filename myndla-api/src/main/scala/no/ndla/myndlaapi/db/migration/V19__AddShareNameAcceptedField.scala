/*
 * Part of NDLA myndla-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.db.migration

import io.circe.{Json, parser}
import no.ndla.database.DocumentMigration

class V19__AddShareNameAcceptedField extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "my_ndla_users"

  override def convertColumn(document: String): String = {
    val oldDocument = parser.parse(document).toTry.get
    oldDocument.mapObject(_.add("shareNameAccepted", Json.False)).noSpaces
  }

}
