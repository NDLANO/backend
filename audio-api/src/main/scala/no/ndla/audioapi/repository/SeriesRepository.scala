/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.model.domain.{AudioMetaInformation, Series}
import no.ndla.audioapi.model.domain
import org.postgresql.util.PGobject
import scalikejdbc.*
import cats.implicits.*
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.database.DataSource

import scala.util.{Failure, Success, Try}

trait SeriesRepository {
  this: DataSource & Props & ErrorHandling =>
  lazy val seriesRepository: SeriesRepository

  class SeriesRepository extends StrictLogging with Repository[Series] {

    /** Method to fetch single series from database
      * @param id
      *   Id of series
      * @param includeEpisodes
      *   Whether to fetch episodes connected to the series. This is slightly more expensive, but usually what we want.
      * @return
      *   Try which decides whether the fetch was successful or not, containing an Option with the series if it was
      *   found, or `None` if it was not.
      */
    def withId(id: Long, includeEpisodes: Boolean = true): Try[Option[Series]] = {
      if (includeEpisodes)
        serieWhere(sqls"se.id = $id")
      else
        serieWhereNoEpisodes(sqls"se.id = $id")
    }

    def deleteWithId(id: Long)(implicit session: DBSession = AutoSession): Try[Int] = {
      Try {
        sql"""
           delete from ${Series.table}
           where id=$id
           """
          .update()
      }
    }

    def update(series: domain.Series)(implicit session: DBSession = AutoSession): Try[domain.Series] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(CirceUtil.toJsonString(series))

      val newRevision = series.revision + 1

      Try(
        sql"""
            update ${Series.table}
            set document=$dataObject, revision=$newRevision
            where id=${series.id} and revision=${series.revision}
           """
          .update()
      ) match {
        case Failure(ex)                  => Failure(ex)
        case Success(count) if count != 1 =>
          val message =
            s"Found revision mismatch when attempting to update series with id '${series.id}' (rev: ${series.revision})"
          logger.info(message)
          Failure(new Helpers.OptimisticLockException)
        case Success(_) =>
          logger.info(s"Updated series with id ${series.id}")
          Success(series.copy(revision = newRevision))
      }
    }

    def insert(newSeries: domain.SeriesWithoutId)(implicit session: DBSession = AutoSession): Try[domain.Series] = {
      val startRevision = 1
      val dataObject    = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(CirceUtil.toJsonString(newSeries))

      Try(
        sql"""
           insert into ${Series.table}(document, revision)
           values ($dataObject, $startRevision)
           """
          .updateAndReturnGeneratedKey()
      ).map(id => Series.fromId(id, startRevision, newSeries))
    }

    override def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): Try[(Long, Long)] = {
      Try(
        sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Series.table}"
          .map(rs => {
            (rs.long("mi"), rs.long("ma"))
          })
          .single() match {
          case Some(minmax) => minmax
          case None         => (0L, 0L)
        }
      )
    }

    override def documentsWithIdBetween(min: Long, max: Long): Try[List[Series]] = {
      seriesWhere(sqls"se.id between $min and $max")
    }

    private def serieWhereNoEpisodes(whereClause: SQLSyntax)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Try[Option[Series]] = {
      val se = Series.syntax("se")

      Try(
        sql"""
           select ${se.result.*}
           from ${Series.as(se)}
           where $whereClause
           """
          .map(Series.fromResultSet(se.resultName))
          .single()
      ).flatMap(_.sequence)
    }

    private def serieWhere(whereClause: SQLSyntax)(implicit
        session: DBSession = ReadOnlyAutoSession
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
            series.map(_.copy(episodes = Some(audios.sortBy(_.created)(Ordering[NDLADate].reverse).toSeq)))
          }
          .single()
      ).flatMap(_.sequence)
    }

    private def seriesWhere(whereClause: SQLSyntax)(implicit
        session: DBSession = ReadOnlyAutoSession
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
      ).flatMap(_.sequence)
    }

  }
}
