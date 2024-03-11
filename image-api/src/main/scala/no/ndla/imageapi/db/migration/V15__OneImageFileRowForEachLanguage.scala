/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migration

import no.ndla.language.Language
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.JsonMethods.parse
import org.json4s.native.Serialization
import org.json4s.*
import org.postgresql.util.PGobject
import scalikejdbc._

class V15__OneImageFileRowForEachLanguage extends BaseJavaMigration {
  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { implicit session =>
      imagesToUpdate.foreach(image => convertImageUpdate(image))
    }

  case class V13__FileRow(
      id: Long,
      fileName: String,
      metadata: String
  )
  case class V13__ImageRow(
      id: Long,
      metadata: String,
      files: Seq[V13__FileRow]
  )

  def imagesToUpdate(implicit session: DBSession): List[V13__ImageRow] = {
    sql"""
        SELECT
          md.id as imageId,
          md.metadata as imageMetadata,
          fd.id as fileId,
          fd.file_name as fileName,
          fd.metadata as fileMetadata
        FROM imagemetadata md
        LEFT JOIN imagefiledata fd
        ON fd.image_meta_id = md.id
       """
      .one(rs => {
        (rs.long("imageId"), rs.string("imageMetadata"))
      })
      .toMany(rs => {
        for {
          id       <- rs.longOpt("fileId")
          fileName <- rs.stringOpt("fileName")
          metadata <- rs.stringOpt("fileMetadata")
        } yield V13__FileRow(id = id, fileName = fileName, metadata = metadata)
      })
      .map((image, imageFiles) => {
        V13__ImageRow(
          id = image._1,
          metadata = image._2,
          files = imageFiles.toSeq
        )
      })
      .list()
  }

  case class V13__LanguageObject(language: String)
  case class V13__ImageDimensions(width: Int, height: Int)
  case class V13__Image(
      size: Long,
      contentType: String,
      dimensions: Option[V13__ImageDimensions],
      language: String
  )

  implicit val formats: Formats =
    org.json4s.DefaultFormats +
      FieldSerializer[V13__Image](ignore("fileName"))

  def insertImageFileData(images: List[(V13__Image, String)], imageId: Long)(implicit session: DBSession): Unit = {
    images.foreach(image => {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      val jsonString = Serialization.write(image._1)
      dataObject.setValue(jsonString)

      sql"""
         insert into imagefiledata(file_name, metadata, image_meta_id)
         values (${image._2}, $dataObject, $imageId)
       """
        .update()
    })
  }

  def convertImageUpdate(image: V13__ImageRow)(implicit session: DBSession): Unit = {
    val oldImage = parse(image.metadata)

    val supportedLanguages = (
      (oldImage \ "titles") ++
        (oldImage \ "alttexts") ++
        (oldImage \ "tags") ++
        (oldImage \ "captions")
    ).extract[List[V13__LanguageObject]]
      .map(_.language)
      .distinct
      .toSet

    val parsedFiles = image.files.map(file => {
      val parsed   = parse(file.metadata)
      val language = (parsed \ "language").extract[String]
      (file, parsed, language)
    })

    val existingImageFiles = parsedFiles.map { case (_, _, language) => language }.toSet
    val missingLanguages   = supportedLanguages -- existingImageFiles

    val fallbackFile = parsedFiles
      .sortBy { case (_, _, language) => Language.languagePriority.reverse.indexOf(language) }
      .reverse
      .headOption

    fallbackFile match {
      case None => println(s"ERROR: No language was found for ${image.id}, bad data, fix manually.")
      case Some((file, fileMetadata, oldLanguage)) =>
        val fallbackImageFile = fileMetadata.extract[V13__Image]
        val missingFiles = missingLanguages.toList.map(language => {
          println(s"Inserting imagefiledata '$language' as copy of '$oldLanguage' for imageId '${image.id}'")
          (fallbackImageFile.copy(language = language), file.fileName)
        })
        insertImageFileData(missingFiles, image.id)
    }
  }

}
