/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.parser
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import no.ndla.database.DocumentMigration
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode

class V44__AddTargetAndRelToAnchors extends DocumentMigration {
  override val tableName: String  = "learningsteps"
  override val columnName: String = "document"

  override def convertColumn(value: String): String = {
    val oldDocument = parser.parse(value).toTry.get
    val description = oldDocument.hcursor
      .downField("description")
      .as[Option[List[OldDescription]]]
      .toTry
      .get
      .getOrElse(List.empty)

    val newDescription = description.map(d => convertDescription(d))

    val newDocument = oldDocument.hcursor
      .withFocus {
        _.mapObject { obj =>
          obj
            .remove("description")
            .add("description", newDescription.asJson)
        }
      }

    newDocument.top.get.noSpaces
  }

  def convertHtml(str: String): String = {
    val document = Jsoup.parseBodyFragment(str)
    document
      .outputSettings()
      .escapeMode(EscapeMode.xhtml)
      .prettyPrint(false)
      .indentAmount(0)

    document
      .select("a")
      .forEach(anchor => {
        anchor.attr("target", "_blank"): Unit
        anchor.attr("rel", "noopener noreferrer"): Unit
      })
    document.select("body").first().html()
  }

  private def convertDescription(description: OldDescription): OldDescription = {
    val convertedHtmlDescription = convertHtml(description.description)
    description.copy(description = convertedHtmlDescription)
  }
}

case class OldDescription(description: String, language: String)
