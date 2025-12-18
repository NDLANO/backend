/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.audioapi.model.api.OptimisticLockException
import no.ndla.audioapi.model.domain.{AudioMetaInformation, Series}
import no.ndla.common.CirceUtil
import no.ndla.network.tapir.ErrorHelpers
import org.postgresql.util.PGobject
import scalikejdbc.*
import no.ndla.database.TrySql.tsql

import scala.util.{Failure, Success, Try}

class AudioRepository(using errorHelpers: ErrorHelpers) extends StrictLogging with Repository[AudioMetaInformation] {
  def audioCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
    tsql"select count(*) from ${AudioMetaInformation.table}"
      .map(rs => rs.long("count"))
      .runSingle()
      .map(_.getOrElse(0L))
      .get

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
    val audioId       = tsql"insert into audiodata (document, revision) values ($dataObject, $startRevision)"
      .updateAndReturnGeneratedKey()
      .get
    audioMetaInformation.copy(id = Some(audioId), revision = Some(startRevision))
  }

  def insertFromImport(audioMetaInformation: AudioMetaInformation, externalId: String): Try[AudioMetaInformation] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(audioMetaInformation))

    DB localTx { implicit session =>
      val startRevision = 1
      tsql"insert into audiodata(external_id, document, revision) values($externalId, $dataObject, $startRevision)"
        .updateAndReturnGeneratedKey()
        .map(id => audioMetaInformation.copy(id = Some(id), revision = Some(startRevision)))
    }
  }

  def update(audioMetaInformation: AudioMetaInformation, id: Long): Try[AudioMetaInformation] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(audioMetaInformation))

    DB localTx { implicit session =>
      val newRevision = audioMetaInformation.revision.getOrElse(0) + 1

      tsql"""
             update audiodata
             set document = $dataObject, revision = $newRevision
             where id = $id and revision = ${audioMetaInformation.revision}
             """
        .update()
        .flatMap {
          case count if count != 1 =>
            val message = s"Found revision mismatch when attempting to update audio with id $id"
            logger.info(message)
            Failure(OptimisticLockException.default)
          case _ =>
            logger.info(s"Updated audio with id $id")
            Success(audioMetaInformation.copy(id = Some(id), revision = Some(newRevision)))
        }
    }
  }

  def setSeriesId(audioMetaId: Long, seriesId: Option[Long])(implicit session: DBSession = AutoSession): Try[Long] = {
    tsql"""
           update ${AudioMetaInformation.table}
           set series_id = $seriesId
           where id = $audioMetaId
           """.update().map(_ => audioMetaId)
  }

  def numElements: Int = {
    DB readOnly { implicit session =>
      tsql"select count(*) from audiodata".map(rs => rs.int("count")).runSingle().map(_.getOrElse(0)).get
    }
  }

  override def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): Try[(Long, Long)] = {
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from audiodata"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
  }

  def deleteAudio(audioId: Long)(implicit session: DBSession = AutoSession): Int = {
    tsql"delete from ${AudioMetaInformation.table} where id=$audioId".update().get
  }

  override def documentsWithIdBetween(min: Long, max: Long): Try[List[AudioMetaInformation]] = {
    audioMetaInformationsWhere(sqls"au.id between $min and $max")
  }

  private def audioMetaInformationWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession): Option[AudioMetaInformation] = {
    val au = AudioMetaInformation.syntax("au")
    val se = Series.syntax("se")
    tsql"""
           select ${au.result.*}, ${se.result.*}
           from ${AudioMetaInformation.as(au)}
           left join ${Series.as(se)} on ${au.seriesId} = ${se.id}
           where $whereClause
         """
      .map { rs =>
        val audio  = AudioMetaInformation.fromResultSet(au)(rs)
        val series = Series.fromResultSet(se)(rs).toOption
        audio.copy(series = series)
      }
      .runList()
      .get
      .headOption
  }

  private def audioMetaInformationsWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Try[List[AudioMetaInformation]] = {
    val au = AudioMetaInformation.syntax("au")
    val se = Series.syntax("se")
    tsql"""
           select ${au.result.*}, ${se.result.*}
           from ${AudioMetaInformation.as(au)}
           left join ${Series.as(se)} on ${au.seriesId} = ${se.id}
           where $whereClause
         """
      .map { rs =>
        val audio  = AudioMetaInformation.fromResultSet(au)(rs)
        val series = Series.fromResultSet(se)(rs).toOption
        audio.copy(series = series)
      }
      .runList()
  }

  def getRandomAudio()(implicit session: DBSession = ReadOnlyAutoSession): Option[AudioMetaInformation] = {
    val au = AudioMetaInformation.syntax("au")
    tsql"select ${au.result.*} from ${AudioMetaInformation.as(au)} tablesample public.system_rows(1)"
      .map(AudioMetaInformation.fromResultSet(au))
      .runSingle()
      .get
  }

  def getByPage(pageSize: Int, offset: Int)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): Seq[AudioMetaInformation] = {
    val au = AudioMetaInformation.syntax("au")
    tsql"""
           select ${au.result.*}
           from ${AudioMetaInformation.as(au)}
           where document is not null
           order by id
           offset $offset
           limit $pageSize
      """.map(AudioMetaInformation.fromResultSet(au)).runList().get
  }

}
