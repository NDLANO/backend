/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.Tag
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.model.api.NotFoundException
import no.ndla.conceptapi.model.domain.{DBConcept, PublishedConcept}
import org.postgresql.util.PGobject
import scalikejdbc.*
import no.ndla.database.implicits.*

import scala.util.{Failure, Success, Try}

class PublishedConceptRepository extends StrictLogging with Repository[Concept] {

  def insertOrUpdate(concept: Concept)(implicit session: DBSession = AutoSession): Try[Concept] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(concept))

    tsql"""update ${PublishedConcept.table}
              set
                document=$dataObject,
                revision=${concept.revision}
              where id=${concept.id}
          """.update() match {
      case Success(count) if count == 1 =>
        logger.info(s"Updated published concept ${concept.id}")
        Success(concept)
      case Success(_) =>
        logger.info(s"No published concept with id ${concept.id} exists, creating...")
        tsql"""
                  insert into ${PublishedConcept.table} (id, document, revision)
                  values (${concept.id}, $dataObject, ${concept.revision})
              """.updateAndReturnGeneratedKey().map(_ => concept)
      case Failure(ex) => Failure(ex)
    }
  }

  def delete(id: Long)(implicit session: DBSession = AutoSession): Try[?] = {
    tsql"""
            delete from ${PublishedConcept.table}
            where id=$id
         """.update() match {
      case Success(count) if count > 0 => Success(id)
      case Failure(ex)                 => Failure(ex)
      case _                           => Failure(NotFoundException("Could not find concept to delete from Published concepts table."))
    }
  }

  def withId(id: Long): Option[Concept] = conceptWhere(sqls"co.id=${id.toInt}")

  def everyTagFromEveryConcept(implicit session: DBSession = ReadOnlyAutoSession): List[List[Tag]] = {
    tsql"""
           select distinct id, document#>'{tags}' as tags
           from ${PublishedConcept.table}
           where jsonb_array_length(document#>'{tags}') > 0
           order by id
         """
      .map(rs => {
        val jsonStr = rs.string("tags")
        CirceUtil.unsafeParseAs[List[Tag]](jsonStr)
      })
      .runList()
      .get
  }

  private def conceptWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Option[Concept] = {
    val co = PublishedConcept.syntax("co")
    tsql"select ${co.result.*} from ${PublishedConcept.as(co)} where co.document is not NULL and $whereClause"
      .map(DBConcept.fromResultSet(co))
      .runSingle()
      .get
  }

  def conceptCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
    tsql"select count(*) from ${PublishedConcept.table}"
      .map(rs => rs.long("count"))
      .runSingle()
      .map(_.getOrElse(0L))
      .get

  override def documentsWithIdBetween(min: Long, max: Long): List[Concept] =
    conceptsWhere(sqls"co.id between $min and $max")

  override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${PublishedConcept.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
      .get
  }

  private def conceptsWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): List[Concept] = {
    val co = PublishedConcept.syntax("co")
    tsql"select ${co.result.*} from ${PublishedConcept.as(co)} where co.document is not NULL and $whereClause"
      .map(DBConcept.fromResultSet(co))
      .runList()
      .get
  }

  def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Concept] = {
    val co = PublishedConcept.syntax("co")
    tsql"""
           select ${co.result.*}
           from ${PublishedConcept.as(co)}
           where document is not null
           order by ${co.id}
           offset $offset
           limit $pageSize
      """.map(DBConcept.fromResultSet(co)).runList().get
  }
}
