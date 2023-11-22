/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.repository

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.NDLADate
import no.ndla.imageapi.integration.DataSource
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.service.ConverterService
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.Try

trait ImageRepository {
  this: DataSource with ConverterService with DBImageMetaInformation with DBImageFile =>
  val imageRepository: ImageRepository

  class ImageRepository extends StrictLogging with Repository[ImageMetaInformation] {
    implicit val formats: Formats =
      ImageMetaInformation.repositorySerializer ++ JavaTimeSerializers.all + NDLADate.Json4sSerializer

    def imageCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
      sql"select count(*) from ${ImageMetaInformation.table}"
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0)

    def withId(id: Long): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.id = $id")
      }
    }

    def withIds(ids: List[Long]): List[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationsWhere(sqls"im.id in ($ids)")
      }
    }

    def withExternalId(externalId: String): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.external_id = $externalId")
      }
    }

    def insert(imageMeta: ImageMetaInformation)(implicit session: DBSession = AutoSession): ImageMetaInformation = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(imageMeta))

      val imageId =
        sql"insert into imagemetadata(metadata) values ($dataObject)".updateAndReturnGeneratedKey()
      imageMeta.copy(id = Some(imageId))
    }

    def update(imageMetaInformation: ImageMetaInformation, id: Long)(implicit
        session: DBSession = AutoSession
    ): Try[ImageMetaInformation] = {
      Try {
        val json       = write(imageMetaInformation)
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(json)
        sql"update imagemetadata set metadata = $dataObject where id = $id".update()
      }.flatMap(_ =>
        imageMetaInformation.images
          .map(updateImageFileMeta)
          .sequence
          .map(_ => imageMetaInformation.copy(id = Some(id)))
      )
    }

    private def updateImageFileMeta(imageFileData: ImageFileData)(implicit session: DBSession): Try[_] =
      Try {
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        val jsonString = write(imageFileData.toDocument())
        dataObject.setValue(jsonString)
        sql"""
            update imagefiledata
            set file_name=${imageFileData.fileName},
                metadata=$dataObject
            where id=${imageFileData.id}
         """
          .update()
      }

    def delete(imageId: Long)(implicit session: DBSession = AutoSession): Int = {
      sql"delete from imagemetadata where id = $imageId".update()
    }

    def deleteImageFileMeta(imageId: Long, language: String)(implicit session: DBSession = AutoSession): Try[Int] = {
      Try(sql"""
            delete from imagefiledata
            where image_meta_id = $imageId
            and metadata->>'language' = $language
         """.update())
    }

    def minMaxId: (Long, Long) = {
      DB readOnly { implicit session =>
        sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from imagemetadata"
          .map(rs => {
            (rs.long("mi"), rs.long("ma"))
          })
          .single() match {
          case Some(minmax) => minmax
          case None         => (0L, 0L)
        }
      }
    }

    def insertImageFile(imageId: Long, fileName: String, document: ImageFileDataDocument)(implicit
        session: DBSession = AutoSession
    ): Try[ImageFileData] = Try {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      val jsonString = write(document)
      dataObject.setValue(jsonString)

      val insertedId =
        sql"""
              insert into imagefiledata(file_name, metadata, image_meta_id)
              values ($fileName, $dataObject, $imageId)
           """
          .updateAndReturnGeneratedKey()

      document.toFull(insertedId, fileName, imageId)
    }

    def documentsWithIdBetween(min: Long, max: Long): List[ImageMetaInformation] =
      imageMetaInformationsWhere(sqls"im.id between $min and $max")

    private def imageMetaInformationWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Option[ImageMetaInformation] = {
      val im  = ImageMetaInformation.syntax("im")
      val dif = Image.syntax("dif")
      sql"""
            SELECT ${im.result.*}, ${dif.result.*}
            FROM ${ImageMetaInformation.as(im)}
            LEFT JOIN ${Image.as(dif)} ON ${dif.imageMetaId} = ${im.id}
            WHERE $whereClause
         """
        .one(ImageMetaInformation.fromResultSet(im.resultName))
        .toMany(rs => Image.fromResultSet(dif.resultName)(rs).toOption.flatten)
        .map((meta, images) => meta.copy(images = images.toSeq))
        .single()
    }

    private def imageMetaInformationsWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): List[ImageMetaInformation] = {
      val im  = ImageMetaInformation.syntax("im")
      val dif = Image.syntax("dif")
      sql"""
            SELECT ${im.result.*}, ${dif.result.*}
            FROM ${ImageMetaInformation.as(im)}
            LEFT JOIN ${Image.as(dif)} ON ${dif.imageMetaId} = ${im.id}
            WHERE $whereClause
         """
        .one(ImageMetaInformation.fromResultSet(im.resultName))
        .toMany(rs => Image.fromResultSet(dif.resultName)(rs).toOption.flatten)
        .map((meta, files) => meta.copy(images = files.toSeq))
        .list()
    }

    def withAndWithoutPrefixSlash(str: String): (String, String) = {
      val without = str.dropWhile(_ == '/')
      (without, s"/$without")
    }

    def getImageFromFilePath(
        filePath: String
    )(implicit session: DBSession = ReadOnlyAutoSession): Option[ImageMetaInformation] = {
      val i                         = Image.syntax("i")
      val (withoutSlash, withSlash) = withAndWithoutPrefixSlash(filePath)
      imageMetaInformationWhere(sqls"""
         im.id = (
           SELECT ${i.imageMetaId}
           FROM ${Image.as(i)}
           WHERE (${i.fileName} = $withSlash OR ${i.fileName} = $withoutSlash)
         )
      """)
    }

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${ImageMetaInformation.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    def getByPage(pageSize: Int, offset: Int)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Seq[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"""
           select ${im.result.*}
           from ${ImageMetaInformation.as(im)}
           where metadata is not null
           order by ${im.id}
           offset $offset
           limit $pageSize
      """
        .map(ImageMetaInformation.fromResultSet(im))
        .list()
    }

  }
}
