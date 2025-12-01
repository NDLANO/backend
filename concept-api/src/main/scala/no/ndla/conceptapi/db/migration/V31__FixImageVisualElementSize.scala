/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.db.migration

import io.circe.parser
import no.ndla.database.DocumentMigration
import scala.util.Try

class V31__FixImageVisualElementSize extends DocumentMigration {
  override val tableName  = "conceptdata"
  override val columnName = "document"

  override def convertColumn(value: String): String = {
    Try {
      val doc            = parser.parse(value).toTry.get
      val visualElements = doc.hcursor.downField("visualElement")
      if (visualElements.succeeded) {
        val updated = visualElements.withFocus(
          _.mapArray(
            _.map { visualElements =>
              visualElements
                .hcursor
                .downField("visualElement")
                .withFocus(visualElement =>
                  visualElement
                    .asString
                    .map(_.replace("data-size=\"fullbredde\"", "data-size=\"full\""))
                    .map(io.circe.Json.fromString)
                    .getOrElse(visualElement)
                )
                .top
                .getOrElse(visualElements)
            }
          )
        )
        updated
          .focus
          .map(f => doc.hcursor.downField("visualElement").withFocus(_ => f).top.getOrElse(doc).noSpaces)
          .getOrElse(value)
      } else doc.noSpaces
    }.getOrElse(value)
  }
}
