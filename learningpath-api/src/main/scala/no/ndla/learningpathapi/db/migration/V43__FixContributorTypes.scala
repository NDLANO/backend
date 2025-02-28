/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.{ACursor, parser}
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import no.ndla.common.model.domain.{Author, ContributorType}
import no.ndla.database.DocumentMigration

class V43__FixContributorTypes extends DocumentMigration {
  override val tableName: String  = "learningpaths"
  override val columnName: String = "document"

  override def convertColumn(value: String): String = {
    val oldDocument  = parser.parse(value).toTry.get
    val copyright    = oldDocument.hcursor.downField("copyright")
    val contributors = convertList(copyright, "contributors")

    val newDocument = oldDocument.hcursor
      .downField("copyright")
      .withFocus(_.mapObject(_.remove("contributors").add("contributors", contributors.asJson)))
    newDocument.top.get.noSpaces
  }

  private def convertList(cursor: ACursor, fieldName: String): List[Author] = {
    val field = cursor.downField(fieldName)
    if (field.succeeded) {
      field.as[Option[List[OldAuthor]]].toTry.get match {
        case None          => List.empty
        case Some(authors) => convertAuthors(authors)
      }
    } else List.empty
  }

  private def convertAuthors(authors: List[OldAuthor]): List[Author] = {
    authors.map(author => {
      ContributorType.valueOf(author.`type`.toLowerCase) match {
        case None    => Author(ContributorType.mapping(author.`type`.toLowerCase), author.name)
        case Some(a) => Author(a, author.name)
      }
    })
  }
}
case class OldAuthor(`type`: String, name: String)
