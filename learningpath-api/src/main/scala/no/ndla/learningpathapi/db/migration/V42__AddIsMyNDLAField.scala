/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import no.ndla.database.DocumentMigration

import java.util.UUID
import scala.util.Try

class V42__AddIsMyNDLAField extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "learningpaths"
  def convertColumn(document: String): String = {
    val oldLp        = parse(document).toTry.get
    val ownerString  = oldLp.hcursor.downField("owner").as[String].toTry.get
    val isMyNDLAUser = Try(UUID.fromString(ownerString)).isSuccess
    oldLp.mapObject(_.add("isMyNDLAOwner", isMyNDLAUser.asJson)).noSpaces
  }
}
