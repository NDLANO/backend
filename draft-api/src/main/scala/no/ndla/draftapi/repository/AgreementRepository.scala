/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.draftapi.integration.DataSource
import no.ndla.draftapi.model.domain.{Agreement, DBArticle}
import org.json4s.Formats
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Success, Try}

trait AgreementRepository {
  this: DataSource with DBArticle =>
  val agreementRepository: AgreementRepository

  class AgreementRepository extends StrictLogging with Repository[Agreement] {
    implicit val formats: Formats = DBAgreement.JSonSerializer

    def insert(agreement: Agreement)(implicit session: DBSession = AutoSession): Agreement = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(agreement))

      val agreementId: Long =
        sql"insert into ${DBAgreement.table} (document) values (${dataObject})".updateAndReturnGeneratedKey()

      logger.info(s"Inserted new agreement: $agreementId")
      agreement.copy(id = Some(agreementId))
    }

    def update(agreement: Agreement)(implicit session: DBSession = AutoSession): Try[Agreement] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(agreement))

      sql"update ${DBAgreement.table} set document=${dataObject} where id=${agreement.id}".update(): Unit

      logger.info(s"Updated agreement ${agreement.id}")
      Success(agreement)
    }

    def withId(id: Long): Option[Agreement] =
      agreementWhere(sqls"agr.id=${id.toInt}")

    def delete(id: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from ${DBAgreement.table} where id = $id".update()
    }

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${DBAgreement.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    override def documentsWithIdBetween(min: Long, max: Long): List[Agreement] =
      agreementsWhere(sqls"agr.id between $min and $max")

    private def agreementWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): Option[Agreement] = {
      val agr = DBAgreement.syntax("agr")
      sql"select ${agr.result.*} from ${DBAgreement.as(agr)} where $whereClause"
        .map(DBAgreement.fromResultSet(agr))
        .single()
    }

    private def agreementsWhere(
        whereClause: SQLSyntax
    )(implicit session: DBSession = ReadOnlyAutoSession): List[Agreement] = {
      val agr = DBAgreement.syntax("agr")
      sql"select ${agr.result.*} from ${DBAgreement.as(agr)} where $whereClause"
        .map(DBAgreement.fromResultSet(agr))
        .list()
    }

  }
}
