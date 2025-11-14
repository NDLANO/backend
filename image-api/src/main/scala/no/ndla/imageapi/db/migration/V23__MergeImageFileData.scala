/*
 * Part of NDLA image-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.db.migration

import io.circe.{Json, parser}
import no.ndla.common.CirceUtil
import no.ndla.database.TableMigration
import no.ndla.imageapi.db.migration.V23__MergeImageFileData.{ImageFileRow, ImageMetaRow}
import org.postgresql.util.PGobject
import scalikejdbc.*

class V23__MergeImageFileData extends TableMigration[ImageMetaRow] {
  override val tableName: String            = "imagemetadata"
  override lazy val whereClause: SQLSyntax  = sqls"metadata is not null"
  private val imageFileTableName: SQLSyntax = sqls"imagefiledata"

  override def extractRowData(rs: WrappedResultSet): ImageMetaRow = ImageMetaRow(rs.long("id"), rs.string("metadata"))

  def convertImageMetadata(imageMetadata: String, imageFileRows: Seq[ImageFileRow]): String = {
    val imageFileJsons = imageFileRows.map { case ImageFileRow(_, fileName, fileMeta) =>
      val fileJson = CirceUtil.tryParse(fileMeta).get
      fileJson.mapObject(_.add("fileName", Json.fromString(fileName)))
    }
    val imageFilesJson = Json.fromValues(imageFileJsons)

    val oldImageMeta = parser.parse(imageMetadata).toTry.get
    oldImageMeta.mapObject(_.add("images", imageFilesJson)).noSpaces
  }

  override def updateRow(imageMetaRow: ImageMetaRow)(implicit session: DBSession): Int = {
    val imageFileRows =
      sql"select * from $imageFileTableName where image_meta_id = ${imageMetaRow.id} and metadata is not null"
        .map(rs => ImageFileRow(rs.long("id"), rs.string("file_name"), rs.string("metadata")))
        .list()

    val newImageMetadata = convertImageMetadata(imageMetaRow.metadata, imageFileRows)

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(newImageMetadata)

    sql"update $tableNameSQL set metadata = $dataObject where id = ${imageMetaRow.id}".update()
  }
}

object V23__MergeImageFileData {
  case class ImageMetaRow(id: Long, metadata: String)

  case class ImageFileRow(imageFileId: Long, imageFileName: String, imageFileMetadata: String)
}
