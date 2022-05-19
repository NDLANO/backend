/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.repository

import no.ndla.frontpageapi.integration.DataSource
import no.ndla.frontpageapi.model.domain.{DBSubjectFrontPageData, SubjectFrontPageData}
import org.log4s.getLogger
import org.postgresql.util.PGobject
import scalikejdbc._
import io.circe.syntax._
import io.circe.generic.auto._

import scala.util.{Failure, Success, Try}

trait SubjectPageRepository {
  this: DataSource with DBSubjectFrontPageData =>
  val subjectPageRepository: SubjectPageRepository

  class SubjectPageRepository {
    val logger = getLogger
    import SubjectFrontPageData._

    def newSubjectPage(subj: SubjectFrontPageData, externalId: String)(implicit
        session: DBSession = AutoSession
    ): Try[SubjectFrontPageData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(subj.copy(id = None).asJson.noSpacesDropNull)

      Try(
        sql"insert into ${DBSubjectFrontPageData.table} (document, external_id) values (${dataObject}, ${externalId})"
          .updateAndReturnGeneratedKey()
      ).map(id => {
        logger.info(s"Inserted new subject page: $id")
        subj.copy(id = Some(id))
      })
    }

    def updateSubjectPage(
        subj: SubjectFrontPageData
    )(implicit session: DBSession = AutoSession): Try[SubjectFrontPageData] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(subj.copy(id = None).asJson.noSpacesDropNull)

      Try(sql"update ${DBSubjectFrontPageData.table} set document=${dataObject} where id=${subj.id}".update())
        .map(_ => subj)
    }

    def withId(subjectId: Long): Option[SubjectFrontPageData] =
      subjectPageWhere(sqls"su.id=${subjectId.toInt}")

    def getIdFromExternalId(externalId: String)(implicit sesstion: DBSession = AutoSession): Try[Option[Long]] = {
      Try(
        sql"select id from ${DBSubjectFrontPageData.table} where external_id=${externalId}"
          .map(rs => rs.long("id"))
          .single()
      )
    }

    def exists(subjectId: Long)(implicit sesstion: DBSession = AutoSession): Try[Boolean] = {
      Try(
        sql"select id from ${DBSubjectFrontPageData.table} where id=${subjectId}"
          .map(rs => rs.long("id"))
          .single()
      ).map(_.isDefined)
    }

    private def subjectPageWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Option[SubjectFrontPageData] = {
      val su = DBSubjectFrontPageData.syntax("su")

      Try(
        sql"select ${su.result.*} from ${DBSubjectFrontPageData.as(su)} where su.document is not NULL and $whereClause"
          .map(DBSubjectFrontPageData.fromDb(su))
          .single()
      ) match {
        case Success(Some(Success(s))) => Some(s)
        case Success(Some(Failure(ex))) =>
          ex.printStackTrace()
          None
        case Failure(ex) =>
          ex.printStackTrace()
          None
        case _ => None
      }
    }

  }
}
