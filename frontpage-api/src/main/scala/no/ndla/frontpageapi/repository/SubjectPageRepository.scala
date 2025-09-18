/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.repository

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import org.postgresql.util.PGobject
import scalikejdbc.*
import io.circe.syntax.*
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.frontpageapi.model.domain.DBSubjectPage

import scala.util.{Failure, Success, Try}

class SubjectPageRepository(using dBSubjectPage: DBSubjectPage) extends StrictLogging {
  def newSubjectPage(subj: SubjectPage, externalId: String)(implicit
      session: DBSession = AutoSession
  ): Try[SubjectPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subj.copy(id = None).asJson.noSpacesDropNull)

    Try(
      sql"insert into ${dBSubjectPage.DBSubjectPage.table} (document, external_id) values (${dataObject}, ${externalId})"
        .updateAndReturnGeneratedKey()
    ).map(id => {
      logger.info(s"Inserted new subject page: $id")
      subj.copy(id = Some(id))
    })
  }

  def updateSubjectPage(
      subj: SubjectPage
  )(implicit session: DBSession = AutoSession): Try[SubjectPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subj.copy(id = None).asJson.noSpacesDropNull)

    Try(sql"update ${dBSubjectPage.DBSubjectPage.table} set document=${dataObject} where id=${subj.id}".update())
      .map(_ => subj)
  }

  def all(offset: Int, limit: Int)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): Try[List[SubjectPage]] = {
    val su = dBSubjectPage.DBSubjectPage.syntax("su")
    Try {
      sql"""
            select ${su.result.*}
            from ${dBSubjectPage.DBSubjectPage.as(su)}
            where su.document is not null
            order by su.id
            offset $offset
            limit $limit
         """
        .map(dBSubjectPage.DBSubjectPage.fromDb(su))
        .list()
        .sequence
    }.flatten
  }

  def withId(subjectId: Long): Try[Option[SubjectPage]] =
    subjectPageWhere(sqls"su.id=${subjectId.toInt}")

  def withIds(subjectIds: List[Long], offset: Int, pageSize: Int)(implicit
      session: DBSession = AutoSession
  ): Try[List[SubjectPage]] = Try {
    val su = dBSubjectPage.DBSubjectPage.syntax("su")
    sql"""
          select ${su.result.*}
          from ${dBSubjectPage.DBSubjectPage.as(su)}
          where su.document is not NULL
          and su.id in ($subjectIds)
          offset $offset
          limit $pageSize
         """
      .map(dBSubjectPage.DBSubjectPage.fromDb(su))
      .list()
      .sequence
  }.flatten

  def getIdFromExternalId(externalId: String)(implicit sesstion: DBSession = AutoSession): Try[Option[Long]] = {
    Try(
      sql"select id from ${dBSubjectPage.DBSubjectPage.table} where external_id=${externalId}"
        .map(rs => rs.long("id"))
        .single()
    )
  }

  def exists(subjectId: Long)(implicit sesstion: DBSession = AutoSession): Try[Boolean] = {
    Try(
      sql"select id from ${dBSubjectPage.DBSubjectPage.table} where id=${subjectId}"
        .map(rs => rs.long("id"))
        .single()
    ).map(_.isDefined)
  }

  def totalCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
    sql"select count(*) from ${dBSubjectPage.DBSubjectPage.table} where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  private def subjectPageWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Try[Option[SubjectPage]] = {
    val su = dBSubjectPage.DBSubjectPage.syntax("su")

    Try(
      sql"select ${su.result.*} from ${dBSubjectPage.DBSubjectPage.as(su)} where su.document is not NULL and $whereClause"
        .map(dBSubjectPage.DBSubjectPage.fromDb(su))
        .single()
    ) match {
      case Success(Some(Success(s)))  => Success(Some(s))
      case Success(Some(Failure(ex))) => Failure(ex)
      case Success(None)              => Success(None)
      case Failure(ex)                => Failure(ex)
    }
  }

}
