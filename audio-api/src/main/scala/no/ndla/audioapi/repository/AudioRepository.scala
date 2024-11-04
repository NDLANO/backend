/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.Props
import no.ndla.audioapi.model.api.ErrorHandling
import no.ndla.audioapi.model.domain.{AudioMetaInformation, Series}
import no.ndla.common.CirceUtil
import no.ndla.database.DataSource
import org.postgresql.util.PGobject
import scalikejdbc.*

import scala.util.{Failure, Success, Try}

trait AudioRepository {
  this: DataSource with SeriesRepository with Props with ErrorHandling =>
  val audioRepository: AudioRepository

  class AudioRepository extends StrictLogging with Repository[AudioMetaInformation] {
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def audioCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
      sql"select count(*) from ${AudioMetaInformation.table}"
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0)

    def withId(id: Long): Option[AudioMetaInformation] = {
      DB readOnly { implicit session =>
        audioMetaInformationWhere(sqls"au.id = $id")
      }
    }

    def withIds(ids: List[Long]): Try[List[AudioMetaInformation]] = {
      DB readOnly { implicit session =>
        audioMetaInformationsWhere(sqls"au.id in ($ids)")
      }
    }

    def withExternalId(externalId: String): Option[AudioMetaInformation] = {
      DB readOnly { implicit session =>
        audioMetaInformationWhere(sqls"au.external_id = $externalId")
      }
    }

    def insert(
        audioMetaInformation: AudioMetaInformation
    )(implicit session: DBSession = AutoSession): AudioMetaInformation = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(CirceUtil.toJsonString(audioMetaInformation))

      val startRevision = 1
      val audioId =
        sql"insert into audiodata (document, revision) values ($dataObject, $startRevision)"
          .updateAndReturnGeneratedKey()
      audioMetaInformation.copy(id = Some(audioId), revision = Some(startRevision))
    }

    def insertFromImport(audioMetaInformation: AudioMetaInformation, externalId: String): Try[AudioMetaInformation] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(CirceUtil.toJsonString(audioMetaInformation))

      DB localTx { implicit session =>
        val startRevision = 1
        val audioId =
          sql"insert into audiodata(external_id, document, revision) values($externalId, $dataObject, $startRevision)"
            .updateAndReturnGeneratedKey()
        Success(audioMetaInformation.copy(id = Some(audioId), revision = Some(startRevision)))
      }
    }

    def update(audioMetaInformation: AudioMetaInformation, id: Long): Try[AudioMetaInformation] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(CirceUtil.toJsonString(audioMetaInformation))

      DB localTx { implicit session =>
        val newRevision = audioMetaInformation.revision.getOrElse(0) + 1

        val count =
          sql"""
             update audiodata
             set document = $dataObject, revision = $newRevision
             where id = $id and revision = ${audioMetaInformation.revision}
             """
            .update()
        if (count != 1) {
          val message = s"Found revision mismatch when attempting to update audio with id $id"
          logger.info(message)
          Failure(new Helpers.OptimisticLockException)
        } else {
          logger.info(s"Updated audio with id $id")
          Success(audioMetaInformation.copy(id = Some(id), revision = Some(newRevision)))
        }
      }
    }

    def setSeriesId(audioMetaId: Long, seriesId: Option[Long])(implicit session: DBSession = AutoSession): Try[Long] = {
      Try(
        sql"""
           update ${AudioMetaInformation.table}
           set series_id = $seriesId
           where id = $audioMetaId
           """
          .update()
      ).map(_ => audioMetaId)
    }

    def numElements: Int = {
      DB readOnly { implicit session =>
        sql"select count(*) from audiodata"
          .map(rs => {
            rs.int("count")
          })
          .single() match {
          case Some(count) => count
          case None        => 0
        }
      }
    }

    override def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): Try[(Long, Long)] = {
      Try(
        sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from audiodata"
          .map(rs => {
            (rs.long("mi"), rs.long("ma"))
          })
          .single() match {
          case Some(minmax) => minmax
          case None         => (0L, 0L)
        }
      )
    }

    def deleteAudio(audioId: Long)(implicit session: DBSession = AutoSession): Int = {
      sql"delete from ${AudioMetaInformation.table} where id=$audioId"
        .update()
    }

    override def documentsWithIdBetween(min: Long, max: Long): Try[List[AudioMetaInformation]] = {
      audioMetaInformationsWhere(sqls"au.id between $min and $max")
    }

    private def audioMetaInformationWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession): Option[AudioMetaInformation] = {
      val au = AudioMetaInformation.syntax("au")
      val se = Series.syntax("se")
      sql"""
           select ${au.result.*}, ${se.result.*}
           from ${AudioMetaInformation.as(au)}
           left join ${Series.as(se)} on ${au.seriesId} = ${se.id}
           where $whereClause
         """
        .one(AudioMetaInformation.fromResultSet(au))
        .toOptionalOne(rs => Series.fromResultSet(se)(rs).toOption)
        .map((audio, series) => audio.copy(series = series))
        .single()
    }

    private def audioMetaInformationsWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[AudioMetaInformation]] = {
      val au = AudioMetaInformation.syntax("au")
      val se = Series.syntax("se")
      Try(
        sql"""
           select ${au.result.*}, ${se.result.*}
           from ${AudioMetaInformation.as(au)}
           left join ${Series.as(se)} on ${au.seriesId} = ${se.id}
           where $whereClause
         """
          .one(AudioMetaInformation.fromResultSet(au))
          .toOptionalOne(rs => Series.fromResultSet(se)(rs).toOption)
          .map((audio, series) => audio.copy(series = series))
          .list()
      )
    }

    def getRandomAudio()(implicit session: DBSession = ReadOnlyAutoSession): Option[AudioMetaInformation] = {
      val au = AudioMetaInformation.syntax("au")
      sql"select ${au.result.*} from ${AudioMetaInformation.as(au)} tablesample public.system_rows(1)"
        .map(AudioMetaInformation.fromResultSet(au))
        .single()
    }

    def getByPage(pageSize: Int, offset: Int)(implicit
        session: DBSession = ReadOnlyAutoSession
    ): Seq[AudioMetaInformation] = {
      val au = AudioMetaInformation.syntax("au")
      sql"""
           select ${au.result.*}
           from ${AudioMetaInformation.as(au)}
           where document is not null
           order by id
           offset $offset
           limit $pageSize
      """
        .map(AudioMetaInformation.fromResultSet(au))
        .list()
    }

  }
}
