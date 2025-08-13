/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

import io.circe.parser
import io.circe.syntax.EncoderOps
import io.circe.generic.auto.*
import no.ndla.database.DocumentMigration

case class TagsObject(tags: List[String], language: String)

class V22__SplitTags extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "conceptdata"

  private def convertTags(tags: List[TagsObject]): List[TagsObject] = tags.map { to =>
    val splitTags = to.tags.flatMap(_.split(":")).filterNot(_.isEmpty)
    to.copy(tags = splitTags)
  }

  override def convertColumn(document: String): String = {
    val oldDocument = parser.parse(document).toTry.get
    oldDocument.hcursor.downField("tags").as[Option[List[TagsObject]]].toTry.get match {
      case None       => document
      case Some(tags) =>
        val convertedTags = convertTags(tags).asJson
        val newDocument   = oldDocument.mapObject(_.remove("tags").add("tags", convertedTags))
        newDocument.noSpaces
    }
  }
}
