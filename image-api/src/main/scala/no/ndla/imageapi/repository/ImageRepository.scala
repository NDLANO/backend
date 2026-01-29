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
import no.ndla.imageapi.model.ImageNotFoundException
import no.ndla.imageapi.model.domain.*
import org.postgresql.util.PGobject
import scalikejdbc.*
import no.ndla.database.DBUtility
import no.ndla.database.implicits.*

import scala.util.{Failure, Success, Try}

class ImageRepository(using dbUtility: DBUtility) extends StrictLogging {
  def imageCount(implicit session: DBSession = dbUtility.readOnlySession): Try[Long] =
    tsql"select count(*) from ${ImageMetaInformation.table}".map(rs => rs.long("count")).runSingle().map(_.getOrElse(0))

  def withId(id: Long): Try[Option[ImageMetaInformation]] = dbUtility.readOnly { implicit session =>
    imageMetaInformationWhere(sqls"im.id = $id")
  }

  def withIds(ids: List[Long]): Try[List[ImageMetaInformation]] = dbUtility.readOnly { implicit session =>
    imageMetaInformationsWhere(sqls"im.id in ($ids)")
  }

  def withExternalId(externalId: String): Try[Option[ImageMetaInformation]] = dbUtility.readOnly { implicit session =>
    imageMetaInformationWhere(sqls"im.external_id = $externalId")
  }

  def insert(imageMeta: ImageMetaInformation)(implicit
      session: DBSession = dbUtility.autoSession
  ): Try[ImageMetaInformation] =
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(imageMeta))

    tsql"insert into imagemetadata(metadata) values ($dataObject)"
      .updateAndReturnGeneratedKey()
      .map(id => imageMeta.copy(id = Some(id)))

  def update(imageMetaInformation: ImageMetaInformation, id: Long)(implicit
      session: DBSession = dbUtility.autoSession
  ): Try[ImageMetaInformation] = {
    val json       = CirceUtil.toJsonString(imageMetaInformation)
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)
    tsql"update imagemetadata set metadata = $dataObject where id = $id"
      .update()
      .map(_ => imageMetaInformation.copy(id = Some(id)))
  }

  def delete(imageId: Long)(implicit session: DBSession = dbUtility.autoSession): Try[Int] = Try {
    tsql"delete from imagemetadata where id = $imageId".update().get
  }.flatMap {
    case n if n < 1 =>
      Failure(new ImageNotFoundException(s"Image with id $imageId was not found, and could not be deleted."))
    case n => Success(n)
  }

  def minMaxId: Try[(Long, Long)] = dbUtility.readOnly { implicit session =>
    Try {
      tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from imagemetadata"
        .map(rs => (rs.long("mi"), rs.long("ma")))
        .runSingle()
        .map(_.getOrElse((0L, 0L)))
        .get
    }
  }

  def documentsWithIdBetween(min: Long, max: Long): Try[List[ImageMetaInformation]] =
    imageMetaInformationsWhere(sqls"im.id between $min and $max")

  private def imageMetaInformationWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession): Try[Option[ImageMetaInformation]] = {
    val im = ImageMetaInformation.syntax("im")
    tsql"""
            SELECT ${im.result.*}
            FROM ${ImageMetaInformation.as(im)}
            WHERE $whereClause
         """.map(ImageMetaInformation.fromResultSet(im.resultName)).runSingleFlat()
  }

  private def imageMetaInformationsWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): Try[List[ImageMetaInformation]] = {
    val im = ImageMetaInformation.syntax("im")
    tsql"""
            SELECT ${im.result.*}
            FROM ${ImageMetaInformation.as(im)}
            WHERE $whereClause
         """.map(ImageMetaInformation.fromResultSet(im.resultName)).runListFlat()
  }

  private def withAndWithoutPrefixSlash(str: String): (String, String) = {
    val without = str.dropWhile(_ == '/')
    (without, s"/$without")
  }

  def getImageFromFilePath(
      filePath: String
  )(implicit session: DBSession = dbUtility.readOnlySession): Try[Option[ImageMetaInformation]] = {
    val (withoutSlash, withSlash) = withAndWithoutPrefixSlash(filePath)
    // Cannot use parameters inside the JSON path expression, so we need to send them as a jsonb object to be referenced
    val jsonbVars = new PGobject()
    jsonbVars.setType("jsonb")
    jsonbVars.setValue(s"""{"withoutSlash": "$withoutSlash", "withSlash": "$withSlash"}""")

    val whereClause = sqls"""
            jsonb_path_exists(im.metadata, '$$.images[*] ? (@.fileName == $$withoutSlash || @.fileName == $$withSlash)', $jsonbVars)"""

    imageMetaInformationWhere(whereClause)
  }

  def minMaxId(implicit session: DBSession = dbUtility.autoSession): Try[(Long, Long)] =
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${ImageMetaInformation.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))

  def getRandomImage()(implicit session: DBSession = dbUtility.readOnlySession): Try[Option[ImageMetaInformation]] = {
    val im = ImageMetaInformation.syntax("im")
    tsql"""SELECT ${im.result.*}
           FROM ${ImageMetaInformation.as(im)} TABLESAMPLE public.system_rows(1)
           LIMIT 1""".map(ImageMetaInformation.fromResultSet(im)).runSingle().map(_.sequence).flatten
  }

  def getByPage(pageSize: Int, offset: Int)(implicit
      session: DBSession = dbUtility.readOnlySession
  ): Try[Seq[ImageMetaInformation]] = {
    val im = ImageMetaInformation.syntax("im")
    tsql"""
           select ${im.result.*}
           from ${ImageMetaInformation.as(im)}
           where metadata is not null
           order by ${im.id}
           offset $offset
           limit $pageSize
      """.map(ImageMetaInformation.fromResultSet(im)).runListFlat()
  }

  // TODO: Remove this after completing variants migration of existing images
  def getImageFileBatched(batchSize: Long): Try[Iterator[Seq[ImageMetaInformation]]] =
    val im    = ImageMetaInformation.syntax("im")
    val total = imageCount match {
      case Success(count) => count
      case Failure(ex)    => return Failure(ex)
    }

    val iter = new Iterator[Seq[ImageMetaInformation]] {
      private var cursor = 0L

      override val knownSize: Int = (
        total.toFloat / batchSize.toFloat
      ).ceil.toInt

      override def hasNext: Boolean = cursor < total

      override def next(): Seq[ImageMetaInformation] = {
        if (cursor >= total) throw IllegalStateException("Called `next` while `hasNext` is false")

        val size = batchSize.min(total - cursor)
        dbUtility.readOnly { implicit session =>
          tsql"""
            select ${im.result.*}
            from ${ImageMetaInformation.as(im)}
            where metadata is not null
            order by ${im.id}
            offset $cursor
            limit $size
             """.map(ImageMetaInformation.fromResultSet(im.resultName)).runListFlat()
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

    Success(iter)
}
