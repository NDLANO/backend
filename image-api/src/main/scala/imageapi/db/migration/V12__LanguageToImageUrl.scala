/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package imageapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction, JField, JObject}
import org.postgresql.util.PGobject
import scalikejdbc._

class V12__LanguageToImageUrl extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map { case (id, document) =>
        update(convertImageUpdate(document), id)
      }
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
  }

  case class V12__LanguageObject(language: String)
  case class V12__ImageDimensions(width: Int, height: Int)
  case class V12__Image(
      fileName: String,
      size: Long,
      contentType: String,
      dimensions: Option[V12__ImageDimensions],
      language: String
  )

  def convertImageUpdate(imageMeta: String): String = {
    val oldImage = parse(imageMeta)

    val existingUrl         = (oldImage \ "imageUrl").extract[String]
    val existingSize        = (oldImage \ "size").extract[Long]
    val existingContentType = (oldImage \ "contentType").extract[String]
    val existingDimensions  = (oldImage \ "imageDimensions").extractOpt[V12__ImageDimensions]

    val supportedLanguages = (
      (oldImage \ "titles") ++
        (oldImage \ "alttexts") ++
        (oldImage \ "tags") ++
        (oldImage \ "captions")
    ).extract[List[V12__LanguageObject]]
      .map(_.language)
      .distinct

    val newImages = supportedLanguages.map(lang => {
      V12__Image(
        fileName = existingUrl,
        size = existingSize,
        contentType = existingContentType,
        dimensions = existingDimensions,
        language = lang
      )
    })

    val imageObject =
      JObject(
        JField(
          "images",
          Extraction.decompose(newImages)
        )
      )

    val withoutOldFields = oldImage.removeField {
      case ("imageUrl", _)        => false
      case ("size", _)            => false
      case ("contentType", _)     => false
      case ("imageDimensions", _) => false
      case _                      => true
    }

    val updated = withoutOldFields.merge(imageObject)

    compact(render(updated))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = $dataObject where id = $id".update()
  }
}
