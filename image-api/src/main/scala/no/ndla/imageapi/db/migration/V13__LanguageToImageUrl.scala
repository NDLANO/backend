/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.native.Serialization
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc._

class V13__LanguageToImageUrl extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      imagesToUpdate.foreach { case (id, document) =>
        convertImageUpdate(document, id)
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

  def updateImageMetaData(imagemetadata: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)
    sql"update imagemetadata set metadata = $dataObject where id = $id".update()
  }

  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      FieldSerializer[V12__Image](ignore("fileName"))

  def insertImageFileData(images: List[V12__Image], imageId: Long)(implicit session: DBSession): Unit = {

    images.foreach(image => {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      val jsonString = Serialization.write(image)
      dataObject.setValue(jsonString)

      sql"""
         insert into imagefiledata(file_name, metadata, image_meta_id)
         values (${image.fileName}, $dataObject, $imageId)
       """
        .update()
    })
  }

  def convertImageUpdate(imageMeta: String, id: Long)(implicit session: DBSession): Unit = {

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

    val withoutOldFields = oldImage.removeField {
      case ("imageUrl", _)        => true
      case ("size", _)            => true
      case ("contentType", _)     => true
      case ("imageDimensions", _) => true
      case _                      => false
    }

    updateImageMetaData(compact(render(withoutOldFields)), id): Unit
    insertImageFileData(newImages, id)
  }

}
