/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.repository

import no.ndla.database.DBUtility
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.Tag
import no.ndla.common.model.domain.concept.Concept
import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.api.{ConceptMissingIdException, NotFoundException, OptimisticLockException}
import no.ndla.conceptapi.model.domain.DBConcept
import no.ndla.network.tapir.ErrorHelpers
import org.postgresql.util.PGobject
import scalikejdbc.*
import no.ndla.database.implicits.*

import scala.util.{Failure, Success, Try}

class DraftConceptRepository(using props: Props, errorHelpers: ErrorHelpers, dbUtility: DBUtility, dbConcept: DBConcept)
    extends StrictLogging
    with Repository[Concept] {
  def insert(concept: Concept)(implicit session: DBSession = dbUtility.autoSession): Concept = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(concept))

    val newRevision = 1

    val conceptId: Long = tsql"""
        insert into ${dbConcept.table} (document, revision)
        values ($dataObject, $newRevision)
          """.updateAndReturnGeneratedKey().get

    logger.info(s"Inserted new concept: $conceptId")
    concept.copy(id = Some(conceptId), revision = Some(newRevision))
  }

  def insertwithListingId(concept: Concept, listingId: Long)(implicit
      session: DBSession = dbUtility.autoSession
  ): Concept = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(concept))

    val newRevision = 1

    val conceptId: Long = tsql"""
        insert into ${dbConcept.table} (listing_id, document, revision)
        values ($listingId, $dataObject, $newRevision)
          """.updateAndReturnGeneratedKey().get

    logger.info(s"Inserted new concept: '$conceptId', with listing id '$listingId'")
    concept.copy(id = Some(conceptId))
  }

  def updateWithListingId(concept: Concept, listingId: Long)(implicit
      session: DBSession = dbUtility.autoSession
  ): Try[Concept] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(concept))

    tsql"""
           update ${dbConcept.table}
           set document=$dataObject
           where listing_id=$listingId
         """.updateAndReturnGeneratedKey() match {
      case Success(id) => Success(concept.copy(id = Some(id)))
      case Failure(ex) =>
        logger.warn(s"Failed to update concept with id ${concept.id} and listing id: $listingId: ${ex.getMessage}")
        Failure(ex)
    }
  }

  def everyTagFromEveryConcept(implicit session: DBSession = dbUtility.readOnlySession): List[List[Tag]] = {
    tsql"""
           select distinct id, document#>'{tags}' as tags
           from ${dbConcept.table}
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

  def withListingId(listingId: Long): Option[Concept] = conceptWhere(sqls"co.listing_id=$listingId")

  def insertWithId(concept: Concept)(implicit session: DBSession = dbUtility.autoSession): Try[Concept] = {
    concept.id match {
      case Some(id) =>
        val dataObject = new PGobject()
        dataObject.setType("jsonb")
        dataObject.setValue(CirceUtil.toJsonString(concept))

        val newRevision = 1

        tsql"""
                  insert into ${dbConcept.table} (id, document, revision)
                  values ($id, $dataObject, $newRevision)
               """
          .update()
          .map(_ => {
            logger.info(s"Inserted new concept: $id")
            concept
          })
      case None => Failure(ConceptMissingIdException("Attempted to insert concept without an id."))
    }
  }

  def update(concept: Concept)(implicit session: DBSession = dbUtility.autoSession): Try[Concept] = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(concept))

    concept.id match {
      case None            => Failure(NotFoundException("Can not update "))
      case Some(conceptId) =>
        val newRevision = concept.revision.getOrElse(0) + 1
        val oldRevision = concept.revision

        tsql"""
              update ${dbConcept.table}
              set
                document=$dataObject,
                revision=$newRevision
              where id=$conceptId
              and revision=$oldRevision
              and revision=(select max(revision) from ${dbConcept.table} where id=$conceptId)
            """
          .update()
          .flatMap(updatedRows => failIfRevisionMismatch(updatedRows, concept, newRevision))
          .recoverWith { case ex =>
            logger.warn(s"Failed to update concept with id ${concept.id}: ${ex.getMessage}")
            Failure(ex)
          }
    }
  }

  private def failIfRevisionMismatch(count: Int, concept: Concept, newRevision: Int): Try[Concept] =
    if (count != 1) {
      val message = s"Found revision mismatch when attempting to update concept ${concept.id}"
      logger.info(message)
      Failure(OptimisticLockException.default)
    } else {
      logger.info(s"Updated concept ${concept.id}")
      val updatedConcept = concept.copy(revision = Some(newRevision))
      Success(updatedConcept)
    }

  def withId(id: Long): Option[Concept] = conceptWhere(sqls"co.id=${id.toInt} ORDER BY revision DESC LIMIT 1")

  def exists(id: Long)(implicit session: DBSession = dbUtility.autoSession): Boolean = {
    tsql"select id from ${dbConcept.table} where id=$id".map(rs => rs.long("id")).runSingle().get.isDefined
  }

  def getIdFromExternalId(externalId: String)(implicit session: DBSession = dbUtility.autoSession): Option[Long] = {
    tsql"select id from ${dbConcept.table} where $externalId = any(external_id)"
      .map(rs => rs.long("id"))
      .runSingle()
      .get
  }

  override def minMaxId(implicit session: DBSession = dbUtility.autoSession): (Long, Long) = {
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${dbConcept.table}"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .map(_.getOrElse((0L, 0L)))
      .get
  }

  override def documentsWithIdBetween(min: Long, max: Long): List[Concept] =
    conceptsWhere(sqls"co.id between $min and $max")

  private def conceptWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): Option[Concept] = {
    val co = dbConcept.syntax("co")
    tsql"select ${co.result.*} from ${dbConcept.as(co)} where co.document is not NULL and $whereClause"
      .map(dbConcept.fromResultSet(co))
      .runSingle()
      .get
  }

  private def conceptsWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): List[Concept] = {
    val co = dbConcept.syntax("co")
    tsql"select ${co.result.*} from ${dbConcept.as(co)} where co.document is not NULL and $whereClause"
      .map(dbConcept.fromResultSet(co))
      .runList()
      .get
  }

  def conceptCount(implicit session: DBSession = dbUtility.readOnlySession): Long =
    tsql"select count(*) from ${dbConcept.table}".map(rs => rs.long("count")).runSingle().map(_.getOrElse(0L)).get

  private def getHighestId(implicit session: DBSession = dbUtility.readOnlySession): Long =
    tsql"select id from ${dbConcept.table} order by id desc limit 1"
      .map(rs => rs.long("id"))
      .runSingle()
      .map(_.getOrElse(0L))
      .get

  def updateIdCounterToHighestId()(implicit session: DBSession = dbUtility.autoSession): Unit = {
    val idToStartAt = SQLSyntax.createUnsafely(
      (
        getHighestId() + 1
      ).toString
    )
    val sequenceName =
      SQLSyntax.createUnsafely(s"${dbConcept.schemaName.getOrElse(props.MetaSchema)}.${dbConcept.tableName}_id_seq")

    val _ = tsql"alter sequence $sequenceName restart with $idToStartAt;".execute()
  }

  def getTags(input: String, pageSize: Int, offset: Int, language: String)(implicit
      session: DBSession = dbUtility.autoSession
  ): (Seq[String], Int) = {
    val sanitizedInput    = input.replaceAll("%", "")
    val sanitizedLanguage = language.replaceAll("%", "")
    val langOrAll         =
      if (sanitizedLanguage == "*" || sanitizedLanguage == "") "%"
      else sanitizedLanguage

    val tags = tsql"""select tags from
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${dbConcept.table}) _
              where tagObj->>'language' like $langOrAll
              order by tags) sorted_tags
              where sorted_tags.tags ilike ${sanitizedInput + '%'}
              offset $offset
              limit $pageSize
                      """.map(rs => rs.string("tags")).runList().get

    val tagsCount = tsql"""
              select count(*) from
              (select distinct JSONB_ARRAY_ELEMENTS_TEXT(tagObj->'tags') tags from
              (select JSONB_ARRAY_ELEMENTS(document#>'{tags}') tagObj from ${dbConcept.table}) _
              where tagObj->>'language' like  $langOrAll) all_tags
              where all_tags.tags ilike ${sanitizedInput + '%'};
           """.map(rs => rs.int("count")).runSingle().map(_.getOrElse(0)).get

    (tags, tagsCount)

  }

  def getByPage(pageSize: Int, offset: Int)(implicit session: DBSession = dbUtility.readOnlySession): Seq[Concept] = {
    val co = dbConcept.syntax("co")
    tsql"""
           select ${co.result.*}, ${co.revision} as revision
           from ${dbConcept.as(co)}
           where document is not null
           order by ${co.id}
           offset $offset
           limit $pageSize
      """.map(dbConcept.fromResultSet(co)).runList().get
  }
}
