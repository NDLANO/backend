/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.integration.DataSource
import no.ndla.conceptapi.model.api.NotFoundException
//import no.ndla.conceptapi.model.api.{NotFoundException, OptimisticLockException}
import no.ndla.conceptapi.model.domain.Concept
import org.json4s.Formats
import org.postgresql.util.PGobject
import org.json4s.native.Serialization.write
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait ConceptRepository {
  this: DataSource =>
  val conceptRepository: ConceptRepository

  class ConceptRepository extends LazyLogging with Repository[Concept] {
    implicit val formats
      : Formats = org.json4s.DefaultFormats + Concept.JSonSerializer

    def insert(concept: Concept)(
        implicit session: DBSession = AutoSession): Concept = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      val conceptId: Long =
        sql"""
        insert into ${Concept.table} (document)
        values (${dataObject})
          """.updateAndReturnGeneratedKey.apply

      logger.info(s"Inserted new concept: $conceptId")
      concept.copy(id = Some(conceptId))
    }

    def update(concept: Concept)(
        implicit session: DBSession = AutoSession): Try[Concept] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      Try(
        sql"update ${Concept.table} set document=${dataObject} where id=${concept.id.get}".updateAndReturnGeneratedKey.apply) match {
        case Success(id) => Success(concept.copy(id = Some(id)))
        case Failure(ex) =>
          logger.warn(
            s"Failed to update concept with id ${concept.id}: ${ex.getMessage}")
          Failure(ex)
      }
    }

    def delete(conceptId: Long)(
        implicit session: DBSession = AutoSession): Try[Long] = {
      val numRows =
        sql"delete from ${Concept.table} where id = $conceptId".update().apply
      if (numRows == 1) {
        Success(conceptId)
      } else {
        Failure(NotFoundException(s"Concept with id $conceptId does not exist"))
      }
    }

    def withId(id: Long): Option[Concept] =
      conceptWhere(sqls"co.id=${id.toInt}")

    def exists(id: Long)(implicit session: DBSession = AutoSession): Boolean = {
      sql"select id from ${Concept.table} where id=${id}"
        .map(rs => rs.long("id"))
        .single
        .apply()
        .isDefined
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession =
                                                  AutoSession): Option[Long] = {
      sql"select id from ${Concept.table} where $externalId = any(external_id)"
        .map(rs => rs.long("id"))
        .single
        .apply()
    }

    override def minMaxId(
        implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Concept.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single()
        .apply() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    override def documentsWithIdBetween(min: Long, max: Long): List[Concept] =
      conceptsWhere(sqls"co.id between $min and $max")

    private def conceptWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where co.document is not NULL and $whereClause"
        .map(Concept(co))
        .single
        .apply()
    }

    private def conceptsWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): List[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where co.document is not NULL and $whereClause"
        .map(Concept(co))
        .list
        .apply()
    }

    def conceptCount(implicit session: DBSession = ReadOnlyAutoSession) =
      sql"select count(*) from ${Concept.table}"
        .map(rs => rs.long("count"))
        .single()
        .apply()
        .getOrElse(0)
//    private def conceptsWhereA(whereClause: SQLSyntax)(
//      implicit session: DBSession = ReadOnlyAutoSession): Seq[Concept] = {
//      val ar = Concept.syntax("ar")
//      sql"select ${ar.result.*} from ${Concept.as(ar)} where ar.document is not NULL and $whereClause"
//        .map(Concept(ar))
//        .list
//        .apply()
//    }
  }
}
