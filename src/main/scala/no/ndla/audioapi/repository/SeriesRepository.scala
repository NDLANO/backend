/*
 * Part of NDLA audio-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.integration.DataSource
import no.ndla.audioapi.model.api.OptimisticLockException
import no.ndla.audioapi.model.domain.{AudioMetaInformation, Series}
import no.ndla.audioapi.model.domain
import org.json4s.Formats
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{DBSession, ReadOnlyAutoSession, _}
import cats.implicits._

import scala.util.{Failure, Success, Try}

trait SeriesRepository {
  this: DataSource =>
  val seriesRepository: SeriesRepository

  class SeriesRepository extends LazyLogging with Repository[Series] {
    val formats: Formats = domain.Series.repositorySerializer
    def withId(id: Long): Try[Option[Series]] = serieWhere(sqls"se.id = $id")

    def deleteWithId(id: Long)(implicit session: DBSession = AutoSession): Try[Int] = {
      Try {
        sql"""
           delete from ${Series.table}
           where id=$id
           """
          .update()
          .apply()
      }
    }

    def update(series: domain.Series)(implicit session: DBSession = AutoSession): Try[domain.Series] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(series)(formats))

      val newRevision = series.revision + 1

      Try(
        sql"""
            update ${Series.table}
            set document=$dataObject, revision=$newRevision
            where id=${series.id} and revision=${series.revision}
           """
          .update()
          .apply()
      ) match {
        case Failure(ex) => Failure(ex)
        case Success(count) if count != 1 =>
          val message =
            s"Found revision mismatch when attempting to update series with id '${series.id}' (rev: ${series.revision})"
          logger.info(message)
          Failure(new OptimisticLockException)
        case Success(_) =>
          logger.info(s"Updated series with id ${series.id}")
          Success(series.copy(revision = newRevision))
      }
    }

    def insert(newSeries: domain.SeriesWithoutId)(implicit session: DBSession = AutoSession): Try[domain.Series] = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(newSeries)(formats))

      Try(
        sql"""
           insert into ${Series.table}(document, revision)
           values ($dataObject, $startRevision)
           """
          .updateAndReturnGeneratedKey()
          .apply()
      ).map(id => Series.fromId(id, startRevision, newSeries))
    }

    override def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): Try[(Long, Long)] = {
      Try(
        sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Series.table}"
          .map(rs => {
            (rs.long("mi"), rs.long("ma"))
          })
          .single()
          .apply() match {
          case Some(minmax) => minmax
          case None         => (0L, 0L)
        }
      )
    }

    override def documentsWithIdBetween(min: Long, max: Long): Try[List[Series]] = {
      seriesWhere(sqls"se.id between $min and $max")
    }

    private def serieWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession
    ): Try[Option[Series]] = {
      val se = Series.syntax("se")
      val au = AudioMetaInformation.syntax("au")

      Try(
        sql"""
           select ${se.result.*}, ${au.result.*}
           from ${Series.as(se)}
           left join ${AudioMetaInformation.as(au)} on ${se.id} = ${au.seriesId}
           where $whereClause
           """
          .one(Series.fromResultSet(se.resultName))
          .toMany(AudioMetaInformation.fromResultSetOpt(au.resultName))
          .map { (series, audios) =>
            series.map(_.copy(episodes = Some(audios.toSeq)))
          }
          .single()
          .apply()
      ).flatMap(_.sequence)
    }

    private def seriesWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession
    ): Try[List[Series]] = {
      val se = Series.syntax("se")
      val au = AudioMetaInformation.syntax("au")

      Try(
        sql"""
           select ${se.result.*}, ${au.result.*}
           from ${Series.as(se)}
           left join ${AudioMetaInformation.as(au)} on ${se.id} = ${au.seriesId}
           where $whereClause
           """
          .one(Series.fromResultSet(se.resultName))
          .toMany(AudioMetaInformation.fromResultSetOpt(au.resultName))
          .map { (series, audios) =>
            series.map(_.copy(episodes = Some(audios.toSeq)))
          }
          .list()
          .apply()
      ).flatMap(_.sequence)
    }

  }
}
