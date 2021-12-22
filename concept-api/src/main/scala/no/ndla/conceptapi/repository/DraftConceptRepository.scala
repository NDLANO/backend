/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.integration.DataSource
import no.ndla.conceptapi.model.api.{ConceptMissingIdException, NotFoundException, OptimisticLockException}
import no.ndla.conceptapi.model.domain.{Concept, ConceptTags}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait DraftConceptRepository {
  this: DataSource =>
  val draftConceptRepository: DraftConceptRepository

  class DraftConceptRepository extends LazyLogging with Repository[Concept] {
    implicit val formats: Formats = Concept.repositorySerializer

    def insert(concept: Concept)(implicit session: DBSession = AutoSession): Concept = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      val newRevision = 1

      val conceptId: Long =
        sql"""
        insert into ${Concept.table} (document, revision)
        values (${dataObject}, $newRevision)
          """.updateAndReturnGeneratedKey()

      logger.info(s"Inserted new concept: $conceptId")
      concept.copy(
        id = Some(conceptId),
        revision = Some(newRevision)
      )
    }

    def insertwithListingId(concept: Concept, listingId: Long)(implicit session: DBSession = AutoSession): Concept = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      val newRevision = 1

      val conceptId: Long =
        sql"""
        insert into ${Concept.table} (listing_id, document, revision)
        values ($listingId, $dataObject, $newRevision)
          """.updateAndReturnGeneratedKey()

      logger.info(s"Inserted new concept: '$conceptId', with listing id '$listingId'")
      concept.copy(id = Some(conceptId))
    }

    def updateWithListingId(concept: Concept, listingId: Long)(
        implicit session: DBSession = AutoSession): Try[Concept] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      Try(
        sql"""
           update ${Concept.table}
           set document=${dataObject}
           where listing_id=${listingId}
         """.updateAndReturnGeneratedKey()
      ) match {
        case Success(id) => Success(concept.copy(id = Some(id)))
        case Failure(ex) =>
          logger.warn(s"Failed to update concept with id ${concept.id} and listing id: $listingId: ${ex.getMessage}")
          Failure(ex)
      }
    }

    def allSubjectIds(implicit session: DBSession = ReadOnlyAutoSession): Set[String] = {
      sql"""
        select distinct jsonb_array_elements_text(document->'subjectIds') as subject_id
        from ${Concept.table}
        where jsonb_array_length(document->'subjectIds') != 0;"""
        .map(rs => rs.string("subject_id"))
        .list()
        .toSet
    }

    def everyTagFromEveryConcept(implicit session: DBSession = ReadOnlyAutoSession) = {
      sql"""
           select distinct id, document#>'{tags}' as tags
           from ${Concept.table}
           where jsonb_array_length(document#>'{tags}') > 0
           order by id
         """
        .map(rs => {
          val jsonStr = rs.string("tags")
          read[List[ConceptTags]](jsonStr)
        })
        .list()
    }

    def withListingId(listingId: Long) =
      conceptWhere(sqls"co.listing_id=$listingId")

    def insertWithId(concept: Concept)(implicit session: DBSession = AutoSession): Try[Concept] = {
      concept.id match {
        case Some(id) =>
          val dataObject = new PGobject()
          dataObject.setType("jsonb")
          dataObject.setValue(write(concept))

          val newRevision = 1

          Try(
            sql"""
                  insert into ${Concept.table} (id, document, revision)
                  values ($id, ${dataObject}, $newRevision)
               """.update()
          )

          logger.info(s"Inserted new concept: $id")
          Success(concept)
        case None =>
          Failure(ConceptMissingIdException("Attempted to insert concept without an id."))
      }
    }

    def update(concept: Concept)(implicit session: DBSession = AutoSession): Try[Concept] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(concept))

      concept.id match {
        case None => Failure(new NotFoundException("Can not update "))
        case Some(conceptId) =>
          val newRevision = concept.revision.getOrElse(0) + 1
          val oldRevision = concept.revision

          Try(
            sql"""
              update ${Concept.table}
              set
                document=${dataObject},
                revision=$newRevision
              where id=$conceptId
              and revision=$oldRevision
              and revision=(select max(revision) from ${Concept.table} where id=$conceptId)
            """.update()
          ) match {
            case Success(updatedRows) => failIfRevisionMismatch(updatedRows, concept, newRevision)
            case Failure(ex) =>
              logger.warn(s"Failed to update concept with id ${concept.id}: ${ex.getMessage}")
              Failure(ex)
          }
      }
    }

    private def failIfRevisionMismatch(count: Int, concept: Concept, newRevision: Int): Try[Concept] =
      if (count != 1) {
        val message = s"Found revision mismatch when attempting to update concept ${concept.id}"
        logger.info(message)
        Failure(new OptimisticLockException)
      } else {
        logger.info(s"Updated concept ${concept.id}")
        val updatedConcept = concept.copy(revision = Some(newRevision))
        Success(updatedConcept)
      }

    def withId(id: Long): Option[Concept] =
      conceptWhere(sqls"co.id=${id.toInt} ORDER BY revision DESC LIMIT 1")

    def exists(id: Long)(implicit session: DBSession = AutoSession): Boolean = {
      sql"select id from ${Concept.table} where id=${id}"
        .map(rs => rs.long("id"))
        .single()
        .isDefined
    }

    def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
      sql"select id from ${Concept.table} where $externalId = any(external_id)"
        .map(rs => rs.long("id"))
        .single()
    }

    override def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Concept.table}"
        .map(rs => {
          (rs.long("mi"), rs.long("ma"))
        })
        .single() match {
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
        .map(Concept.fromResultSet(co))
        .single()
    }

    private def conceptsWhere(whereClause: SQLSyntax)(
        implicit session: DBSession = ReadOnlyAutoSession): List[Concept] = {
      val co = Concept.syntax("co")
      sql"select ${co.result.*} from ${Concept.as(co)} where co.document is not NULL and $whereClause"
        .map(Concept.fromResultSet(co))
        .list()
    }

    def conceptCount(implicit session: DBSession = ReadOnlyAutoSession): Long =
      sql"select count(*) from ${Concept.table}"
        .map(rs => rs.long("count"))
        .single()
        .getOrElse(0)

    private def getHighestId(implicit session: DBSession = ReadOnlyAutoSession): Long = {
      sql"select id from ${Concept.table} order by id desc limit 1"
        .map(rs => rs.long("id"))
        .single()
        .getOrElse(0)
    }

    def updateIdCounterToHighestId()(implicit session: DBSession = AutoSession): Int = {
      val idToStartAt = SQLSyntax.createUnsafely((getHighestId() + 1).toString)
      val sequenceName = SQLSyntax.createUnsafely(
        s"${Concept.schemaName.getOrElse(ConceptApiProperties.MetaSchema)}.${Concept.tableName}_id_seq")

      sql"alter sequence $sequenceName restart with $idToStartAt;".executeUpdate()
    }

    def getTags(input: String, pageSize: Int, offset: Int, language: String)(
        implicit session: DBSession = AutoSession): (Seq[String], Int) = {
      val sanitizedInput = input.replaceAll("%", "")
      val sanitizedLanguage = language.replaceAll("%", "")
      val langOrAll = if (sanitizedLanguage == "*" || sanitizedLanguage == "") "%" else sanitizedLanguage

      val tags = sql"""select tags from
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${Concept.table}) _
              where tagObj->>'language' like ${langOrAll}
              order by tags) sorted_tags
              where sorted_tags.tags ilike ${sanitizedInput + '%'}
              offset ${offset}
              limit ${pageSize}
                      """
        .map(rs => rs.string("tags"))
        .list()

      val tagsCount =
        sql"""
              select count(*) from
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${Concept.table}) _
              where tagObj->>'language' like  ${langOrAll}) all_tags
              where all_tags.tags ilike ${sanitizedInput + '%'};
           """
          .map(rs => rs.int("count"))
          .single()
          .getOrElse(0)

      (tags, tagsCount)

    }

    def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Concept] = {
      val co = Concept.syntax("co")
      sql"""
           select ${co.result.*}, ${co.revision} as revision
           from ${Concept.as(co)}
           where document is not null
           offset $offset
           limit $pageSize
      """
        .map(Concept.fromResultSet(co))
        .list()
    }
  }
}
