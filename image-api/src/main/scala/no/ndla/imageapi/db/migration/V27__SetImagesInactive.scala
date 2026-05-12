/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import io.circe.{Json, parser}
import no.ndla.database.{DocumentMigration, DocumentRow}
import org.postgresql.util.PGobject
import scalikejdbc.*
import scala.io.Source

class V27__SetImagesInactive extends DocumentMigration {
  override val tableName: String  = "imagemetadata"
  override val columnName: String = "metadata"

  private val ids: Set[Long] = {
    val stream = getClass.getResourceAsStream("/v27-migration-image-ids.txt")
    try {
      val content = Source.fromInputStream(stream).mkString.trim
      content
        .split(",")
        .flatMap { part =>
          val trimmed = part.trim
          if (trimmed.contains("-")) {
            // Handle range like "11-14"
            val Array(start, end) = trimmed.split("-").map(_.toLong)
            start to end
          } else {
            // Handle single ID
            Seq(trimmed.toLong)
          }
        }
        .toSet
    } finally {
      if (stream != null) stream.close()
    }
  }

  private val cutoffId = 73000

  override def convertColumn(value: String): String = {
    // This method is not used since we override updateRow
    value
  }

  override def updateRow(rowData: DocumentRow)(implicit session: DBSession): Int = {
    val oldDocument = parser.parse(rowData.document).toTry.get
    val documentId  = rowData.id

    // Condition 1: id is less than cutoffId and not in ids list
    val matchesIdCondition = documentId < cutoffId && !ids.contains(documentId)

    // Get titles array
    val titlesOpt = oldDocument.hcursor.downField("titles").focus.flatMap(_.asArray)

    // Condition 2: any title contains "ikke bruk"
    val matchesTitleFilter = titlesOpt.exists { titles =>
      titles.exists { titleJson =>
        titleJson.hcursor.downField("title").as[String].toOption match {
          case Some(title) => title.toLowerCase().contains("ikke bruk")
          case None        => false
        }
      }
    }

    // Get images array
    val imagesOpt = oldDocument.hcursor.downField("images").focus.flatMap(_.asArray)

    // Condition 3: any image has width less than 990
    val hasSmallImage = imagesOpt.exists { images =>
      images.exists { imageJson =>
        imageJson.hcursor.downField("dimensions").downField("width").as[Int].toOption match {
          case Some(width) => width < 990
          case None        => false
        }
      }
    }

    if (
      !(
        matchesIdCondition || matchesTitleFilter || hasSmallImage
      )
    ) {
      // No conditions met, return 0
      return 0
    }

    // At least one condition met, set inactive to true
    val newDocument = oldDocument.mapObject(_.add("inactive", Json.fromBoolean(true)))

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(newDocument.noSpaces)

    val columnNameSQL = SQLSyntax.createUnsafely(columnName)
    val tableNameSQL  = SQLSyntax.createUnsafely(tableName)

    sql"""update $tableNameSQL
          set $columnNameSQL = $dataObject
          where id = ${rowData.id}
       """.update()
  }

}
