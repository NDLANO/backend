/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.repository

import no.ndla.database.{DBUtility, ReadableDbSession}
import com.typesafe.scalalogging.StrictLogging
import io.circe.syntax.*
import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.database.implicits.*
import no.ndla.frontpageapi.model.domain.DBSubjectPage
import org.postgresql.util.PGobject
import scalikejdbc.*

import java.time.temporal.TemporalQueries.offset
import scala.util.{Success, Failure, Try}

class SubjectPageRepository(using dBSubjectPage: DBSubjectPage, dbUtility: DBUtility) extends StrictLogging {
  def newSubjectPage(subj: SubjectPage, externalId: String)(implicit
      session: DBSession = dbUtility.autoSession
  ): Try[SubjectPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subj.copy(id = None).asJson.noSpacesDropNull)

    tsql"insert into ${dBSubjectPage.DBSubjectPage.table} (document, external_id) values (${dataObject}, ${externalId})"
      .updateAndReturnGeneratedKey()
      .map(id => {
        logger.info(s"Inserted new subject page: $id")
        subj.copy(id = Some(id))
      })
  }

  def updateSubjectPage(subj: SubjectPage)(implicit session: DBSession = dbUtility.autoSession): Try[SubjectPage] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(subj.copy(id = None).asJson.noSpacesDropNull)

    tsql"update ${dBSubjectPage.DBSubjectPage.table} set document=${dataObject} where id=${subj.id}"
      .update()
      .map(_ => subj)
  }

  def all(offset: Int, limit: Int)(implicit session: DBSession = dbUtility.readOnlySession): Try[List[SubjectPage]] = {
    val su = dBSubjectPage.DBSubjectPage.syntax("su")
    tsql"""
            select ${su.result.*}
            from ${dBSubjectPage.DBSubjectPage.as(su)}
            where su.document is not null
            order by su.id
            offset $offset
            limit $limit
         """.map(dBSubjectPage.DBSubjectPage.fromDb(su)).runListFlat()
  }

  def withId(subjectId: Long)(implicit session: DBSession = dbUtility.readOnlySession): Try[Option[SubjectPage]] =
    subjectPageWhere(sqls"su.id=${subjectId.toInt}")

  def withIds(subjectIds: List[Long], offset: Int, pageSize: Int)(implicit
      session: DBSession = dbUtility.autoSession
  ): Try[List[SubjectPage]] = {
    val su = dBSubjectPage.DBSubjectPage.syntax("su")
    tsql"""
          select ${su.result.*}
          from ${dBSubjectPage.DBSubjectPage.as(su)}
          where su.document is not NULL
          and su.id in ($subjectIds)
          offset $offset
          limit $pageSize
         """.map(dBSubjectPage.DBSubjectPage.fromDb(su)).runListFlat()
  }

  def getIdFromExternalId(externalId: String)(implicit sesstion: DBSession = dbUtility.autoSession): Try[Option[Long]] =
    tsql"select id from ${dBSubjectPage.DBSubjectPage.table} where external_id=${externalId}"
      .map(rs => rs.long("id"))
      .runSingle()

  def exists(subjectId: Long)(implicit sesstion: DBSession = dbUtility.autoSession): Try[Boolean] =
    tsql"select id from ${dBSubjectPage.DBSubjectPage.table} where id=${subjectId}"
      .map(rs => rs.long("id"))
      .runSingle()
      .map(_.isDefined)

  def totalCount(implicit session: DBSession = dbUtility.readOnlySession): Try[Long] =
    tsql"select count(*) from ${dBSubjectPage.DBSubjectPage.table} where document is not NULL"
      .map(rs => rs.long("count"))
      .runSingle()
      .map(_.getOrElse(0L))

  private def subjectPageWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): Try[Option[SubjectPage]] = {
    val su = dBSubjectPage.DBSubjectPage.syntax("su")

    tsql"select ${su.result.*} from ${dBSubjectPage.DBSubjectPage.as(su)} where su.document is not NULL and $whereClause"
      .map(dBSubjectPage.DBSubjectPage.fromDb(su))
      .runSingleFlat()
  }

  def subjectPageIterator(implicit session: ReadableDbSession): Try[Iterator[Try[Seq[SubjectPage]]]] = {
    val pageSize = 100L
    val su       = dBSubjectPage.DBSubjectPage.syntax("su")
    val total    = totalCount match {
      case Success(count) => count
      case Failure(ex)    => return Failure(ex)
    }

    Success(
      new Iterator[Try[Seq[SubjectPage]]] {
        private var cursor          = 0L
        override val knownSize: Int = (
          total.toFloat / pageSize.toFloat
        ).ceil.toInt

        override def hasNext: Boolean              = cursor < total
        override def next(): Try[Seq[SubjectPage]] = {
          if (cursor >= total) {
            return Failure(new IllegalStateException("Called `next` while `hasNext` is false"))
          }

          val result = tsql"""
              select ${su.result.*}
              from ${dBSubjectPage.DBSubjectPage.as(su)}
              where su.document is not NULL
              order by ${su.id}
              offset $offset
              limit $pageSize
          """.map(dBSubjectPage.DBSubjectPage.fromDb(su)).runListFlat()
          result.foreach(_ => cursor += pageSize.min(total - cursor))
          result
        }
      }
    )
  }

}
