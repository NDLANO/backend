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
import no.ndla.conceptapi.model.domain.{Concept, ConceptTags, PublishedConcept}
import org.json4s.Formats
import org.postgresql.util.PGobject
import scalikejdbc._
import org.json4s.native.Serialization.{read, write}

import scala.util.{Failure, Success, Try}

trait PublishedConceptRepository {
  this: DataSource =>
  val publishedConceptRepository: PublishedConceptRepository

  class PublishedConceptRepository extends LazyLogging with Repository[Concept] {
    implicit val formats: Formats = Concept.repositorySerializer

    def insertOrUpdate(concept: Concept)(implicit session: DBSession = AutoSession): Try[Concept] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      Try {
        sql"""update ${PublishedConcept.table}
              set
                document=$dataObject,
                revision=${concept.revision}
              where id=${concept.id}
          """.update()
      } match {
        case Success(count) if count == 1 =>
          logger.info(s"Updated published concept ${concept.id}")
          Success(concept)
        case Success(_) =>
          logger.info(s"No published concept with id ${concept.id} exists, creating...")
          Try {
            sql"""
                  insert into ${PublishedConcept.table} (id, document, revision)
                  values (${concept.id}, $dataObject, ${concept.revision})
              """.updateAndReturnGeneratedKey()
          }.map(_ => concept)
        case Failure(ex) => Failure(ex)
      }
    }

    def delete(id: Long)(implicit session: DBSession = AutoSession): Try[_] = {
      Try(
        sql"""
            delete from ${PublishedConcept.table}
            where id=$id
         """.update()
      ) match {
        case Success(count) if count > 0 => Success(id)
        case Failure(ex)                 => Failure(ex)
        case _                           => Failure(NotFoundException("Could not find concept to delete from Published concepts table."))
      }
    }

    def withId(id: Long): Option[Concept] = conceptWhere(sqls"co.id=${id.toInt}")

    def allSubjectIds(implicit session: DBSession = ReadOnlyAutoSession): Set[String] = {
      sql"""
        select distinct jsonb_array_elements_text(document->'subjectIds') as subject_id
        from ${PublishedConcept.table}
        where jsonb_array_length(document->'subjectIds') != 0;"""
        .map(rs => rs.string("subject_id"))
        .list()
        .toSet
    }

    def everyTagFromEveryConcept(implicit session: DBSession = ReadOnlyAutoSession) = {
      sql"""
           select distinct id, document#>'{tags}' as tags
           from ${PublishedConcept.table}
           where jsonb_array_length(document#>'{tags}') > 0
           order by id
         """
        .map(rs => {
          val jsonStr = rs.string("tags")
          read[List[ConceptTags]](jsonStr)
        })
        .list()
    }

    private def conceptWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): Option[Concept] = {
      val co = PublishedConcept.syntax("co")
      sql"select ${co.result.*} from ${PublishedConcept.as(co)} where co.document is not NULL and $whereClause"
        .map(Concept.fromResultSet(co))
        .single()
    }

    def conceptCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
      sql"select count(*) from ${PublishedConcept.table}"
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0)

    override def documentsWithIdBetween(min: Long, max: Long): List[Concept] =
      conceptsWhere(sqls"co.id between $min and $max")

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${PublishedConcept.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single() match {
        case Some(minmax) => minmax
        case None         => (0L, 0L)
      }
    }

    private def conceptsWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): List[Concept] = {
      val co = PublishedConcept.syntax("co")
      sql"select ${co.result.*} from ${PublishedConcept.as(co)} where co.document is not NULL and $whereClause"
        .map(Concept.fromResultSet(co))
        .list()
    }

    def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Concept] = {
      val co = PublishedConcept.syntax("co")
      sql"""
           select ${co.result.*}
           from ${PublishedConcept.as(co)}
           where document is not null
           offset $offset
           limit $pageSize
      """
        .map(Concept.fromResultSet(co))
        .list()
    }
  }
}
