/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.repository

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.imageapi.model.domain.*
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

class ImageRepository extends StrictLogging {
  def imageCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
    sql"select count(*) from ${ImageMetaInformation.table}".map(rs => rs.long("count")).single().getOrElse(0)

  def withId(id: Long): Try[Option[ImageMetaInformation]] = Try {
    DB readOnly { implicit session =>
      imageMetaInformationWhere(sqls"im.id = $id")
    }
  }.flatten

  def withIds(ids: List[Long]): Try[List[ImageMetaInformation]] = Try {
    DB readOnly { implicit session =>
      imageMetaInformationsWhere(sqls"im.id in ($ids)")
    }
  }.flatten

  def withExternalId(externalId: String): Try[Option[ImageMetaInformation]] = Try {
    DB readOnly { implicit session =>
      imageMetaInformationWhere(sqls"im.external_id = $externalId")
    }
  }.flatten

  def insert(imageMeta: ImageMetaInformation)(implicit session: DBSession = AutoSession): ImageMetaInformation = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(imageMeta))

    val imageId = sql"insert into imagemetadata(metadata) values ($dataObject)".updateAndReturnGeneratedKey()
    imageMeta.copy(id = Some(imageId))
  }

  def update(imageMetaInformation: ImageMetaInformation, id: Long)(implicit
      session: DBSession = AutoSession
  ): Try[ImageMetaInformation] = {
    Try {
      val json       = CirceUtil.toJsonString(imageMetaInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)
      sql"update imagemetadata set metadata = $dataObject where id = $id".update()
    }.map(_ => imageMetaInformation.copy(id = Some(id)))
  }

  def delete(imageId: Long)(implicit session: DBSession = AutoSession): Int = {
    sql"delete from imagemetadata where id = $imageId".update()
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

  def documentsWithIdBetween(min: Long, max: Long): Try[List[ImageMetaInformation]] =
    imageMetaInformationsWhere(sqls"im.id between $min and $max")

  private def imageMetaInformationWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession): Try[Option[ImageMetaInformation]] = Try {
    val im = ImageMetaInformation.syntax("im")
    sql"""
            SELECT ${im.result.*}
            FROM ${ImageMetaInformation.as(im)}
            WHERE $whereClause
         """.map(ImageMetaInformation.fromResultSet(im.resultName)).single()
  }

  private def imageMetaInformationsWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[ImageMetaInformation]] = Try {
    val im = ImageMetaInformation.syntax("im")
    sql"""
            SELECT ${im.result.*}
            FROM ${ImageMetaInformation.as(im)}
            WHERE $whereClause
         """.map(ImageMetaInformation.fromResultSet(im.resultName)).list()
  }

  private def withAndWithoutPrefixSlash(str: String): (String, String) = {
    val without = str.dropWhile(_ == '/')
    (without, s"/$without")
  }

  def getImageFromFilePath(
      filePath: String
  )(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[ImageMetaInformation]] = {
    val (withoutSlash, withSlash) = withAndWithoutPrefixSlash(filePath)
    // Cannot use parameters inside the JSON path expression, so we need to send them as a jsonb object to be referenced
    val jsonbVars = new PGobject()
    jsonbVars.setType("jsonb")
    jsonbVars.setValue(s"""{"withoutSlash": "$withoutSlash", "withSlash": "$withSlash"}""")

    val whereClause = sqls"""
            jsonb_path_exists(im.metadata, '$$.images[*] ? (@.fileName == $$withoutSlash || @.fileName == $$withSlash)', $jsonbVars)"""

    imageMetaInformationWhere(whereClause)
  }

  def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
    sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${ImageMetaInformation.table}"
      .map(rs => {
        (rs.long("mi"), rs.long("ma"))
      })
      .single() match {
      case Some(minmax) => minmax
      case None         => (0L, 0L)
    }
  }

  def getRandomImage()(implicit session: DBSession = ReadOnlyAutoSession): Option[ImageMetaInformation] = {
    val im = ImageMetaInformation.syntax("im")
    sql"""SELECT ${im.result.*}
           FROM ${ImageMetaInformation.as(im)} TABLESAMPLE public.system_rows(1)
           LIMIT 1""".map(ImageMetaInformation.fromResultSet(im)).single()
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
      """.map(ImageMetaInformation.fromResultSet(im)).list()
  }

  // TODO: Remove this after completing variants migration of existing images
  def getImageFileBatched(batchSize: Long): Iterator[Seq[ImageMetaInformation]] =
    new Iterator[Seq[ImageMetaInformation]] {
      private val im     = ImageMetaInformation.syntax("im")
      private val total  = imageCount
      private var cursor = 0L

      override val knownSize: Int = (
        total.toFloat / batchSize.toFloat
      ).ceil.toInt

      override def hasNext: Boolean = cursor < total

      override def next(): Seq[ImageMetaInformation] = {
        if (cursor >= total) throw IllegalStateException("Called `next` while `hasNext` is false")

        val size = batchSize.min(total - cursor)
        DB.readOnly { case given DBSession =>
          Try {
            sql"""
            select ${im.result.*}
            from ${ImageMetaInformation.as(im)}
            where metadata is not null
            order by ${im.id}
            offset $cursor
            limit $size
             """.map(ImageMetaInformation.fromResultSet(im.resultName)).list()
          }
        } match {
          case Success(images) =>
            cursor += size
            images
          case Failure(ex) =>
            logger.error("Failed to fetch next batch of ImageMetaInformation", ex)
            throw ex
        }
      }
    }
}
