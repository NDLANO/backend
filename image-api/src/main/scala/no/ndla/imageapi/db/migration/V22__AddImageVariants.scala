/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import io.circe.{Json, parser}
import no.ndla.database.DocumentMigration

class V22__AddImageVariants extends DocumentMigration {
  override val tableName: String  = "imagefiledata"
  override val columnName: String = "metadata"

  override def convertColumn(value: String): String = {
    val oldDocument = parser.parse(value).toTry.get
    oldDocument.mapObject(_.add("variants", Json.arr())).noSpaces
  }
}
