/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import no.ndla.database.DBUtility
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.{Author, Tag}
import no.ndla.common.model.domain.learningpath.{LearningPath, LearningPathStatus, LearningStep, LearningpathCopyright}
import no.ndla.learningpathapi.model.domain.*
import org.postgresql.util.PGobject
import scalikejdbc.*
import no.ndla.database.implicits.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

class LearningPathRepository(using dbUtility: DBUtility) extends StrictLogging {

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A = {
    Option(session) match {
      case Some(x) => work(x)
      case None    => dbUtility.localTx { implicit newSession =>
          work(newSession)
        }
    }
  }

  def withId(id: Long)(implicit session: DBSession = dbUtility.autoSession): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id AND lp.document->>'status' <> ${LearningPathStatus.DELETED.toString}")
  }

  def withIdIncludingDeleted(id: Long)(implicit session: DBSession = dbUtility.autoSession): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id")
  }

  def withExternalId(externalId: String): Option[LearningPath] = {
    learningPathWhere(sqls"lp.external_id = $externalId")
  }

  def withOwner(owner: String): List[LearningPath] = {
    learningPathsWhere(
      sqls"lp.document->>'owner' = $owner AND lp.document->>'status' <> ${LearningPathStatus.DELETED.toString} order by lp.document->>'created' DESC"
    )
  }

  def getIdFromExternalId(externalId: String)(implicit session: DBSession = dbUtility.autoSession): Option[Long] = {
    tsql"select id from learningpaths where external_id = $externalId".map(rs => rs.long("id")).runSingle().get
  }

  def learningPathsWithIsBasedOn(isBasedOnId: Long): List[LearningPath] = {
    learningPathsWhere(sqls"lp.document->>'isBasedOn' = ${isBasedOnId.toString}")
  }

  def learningPathsWithIsBasedOnRaw(isBasedOnId: Long): List[LearningPath] = {
    learningPathsWhereWithInactive(sqls"lp.document->>'isBasedOn' = ${isBasedOnId.toString}")
  }

  def learningStepsFor(
      learningPathId: Long
  )(implicit session: DBSession = dbUtility.readOnlySession): Seq[LearningStep] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where ${lp.id} = $learningPathId"
      .map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).learningsteps)
      .runSingle()
      .get
      .getOrElse(Seq.empty)
  }

  def learningStepWithId(learningPathId: Long, learningStepId: Long)(implicit
      session: DBSession = dbUtility.readOnlySession
  ): Option[LearningStep] = {
    learningStepsFor(learningPathId).find(_.id.contains(learningStepId))
  }

  def insert(learningpath: LearningPath)(implicit session: DBSession = dbUtility.autoSession): Try[LearningPath] = {
    val startRevision  = 1
    val learningPathId = generateLearningPathId
    val toInsert       = withStepIdsForInsert(learningpath, learningPathId, startRevision)
    val dataObject     = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(toInsert))
    tsql"""insert into learningpaths(id, external_id, document, revision)
           values($learningPathId, ${learningpath.externalId}, $dataObject, $startRevision)
        """.update() match {
      case Success(1) =>
        logger.info(s"Inserted learningpath with id $learningPathId")
        Success(toInsert)
      case Success(_)  => Failure(new RuntimeException(s"Failed to insert learningpath with id $learningPathId"))
      case Failure(ex) => Failure(ex)
    }
  }

  def insertLearningStep(
      learningStep: LearningStep
  )(implicit session: DBSession = dbUtility.autoSession): LearningStep = {
    val startRevision = 1
    val stepObject    = new PGobject()
    stepObject.setType("jsonb")
    stepObject.setValue(CirceUtil.toJsonString(learningStep))

    val learningStepId: Long =
      tsql"insert into learningsteps(learning_path_id, external_id, document, revision) values (${learningStep.learningPathId}, ${learningStep.externalId}, $stepObject, $startRevision)"
        .updateAndReturnGeneratedKey()
        .get
    logger.info(s"Inserted learningstep with id $learningStepId")
    learningStep.copy(id = Some(learningStepId), revision = Some(startRevision))
  }

  def update(learningpath: LearningPath)(implicit session: DBSession = dbUtility.autoSession): LearningPath = {
    if (learningpath.id.isEmpty) {
      throw new RuntimeException("A non-persisted learningpath cannot be updated without being saved first.")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(learningpath))

    val newRevision = learningpath.revision.getOrElse(0) + 1
    val count       =
      tsql"update learningpaths set document = $dataObject, revision = $newRevision where id = ${learningpath.id} and revision = ${learningpath.revision}"
        .update()
        .get

    if (count != 1) {
      val msg =
        s"Conflicting revision is detected for learningPath with id = ${learningpath.id} and revision = ${learningpath.revision}"
      logger.warn(msg)
      throw new OptimisticLockException(msg)
    }

    logger.info(s"Updated learningpath with id ${learningpath.id}")
    learningpath.copy(revision = Some(newRevision))
  }

  def updateWithImportId(learningpath: LearningPath, importId: String)(implicit
      session: DBSession = dbUtility.autoSession
  ): LearningPath = {
    if (learningpath.id.isEmpty) {
      throw new RuntimeException("A non-persisted learningpath cannot be updated without being saved first.")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(learningpath))

    val importIdUUID = Try(UUID.fromString(importId)).toOption
    val newRevision  = learningpath.revision.getOrElse(0) + 1
    val count        =
      tsql"update learningpaths set document = $dataObject, revision = $newRevision, import_id = $importIdUUID where id = ${learningpath.id} and revision = ${learningpath.revision}"
        .update()
        .get

    if (count != 1) {
      val msg =
        s"Conflicting revision is detected for learningPath with id = ${learningpath.id} and revision = ${learningpath.revision}"
      logger.warn(msg)
      throw new OptimisticLockException(msg)
    }

    logger.info(s"Updated learningpath with id ${learningpath.id}")
    learningpath.copy(revision = Some(newRevision))
  }

  def updateLearningStep(
      learningStep: LearningStep
  )(implicit session: DBSession = dbUtility.autoSession): LearningStep = {
    if (learningStep.id.isEmpty) {
      throw new RuntimeException("A non-persisted learningStep cannot be updated without being saved first.")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(learningStep))

    val newRevision = learningStep.revision.getOrElse(0) + 1
    val count       =
      tsql"update learningsteps set document = $dataObject, revision = $newRevision where id = ${learningStep.id} and revision = ${learningStep.revision}"
        .update()
        .get
    if (count != 1) {
      val msg =
        s"Conflicting revision is detected for learningStep with id = ${learningStep.id} and revision = ${learningStep.revision}"
      logger.warn(msg)
      throw new OptimisticLockException(msg)
    }

    logger.info(s"Updated learningstep with id ${learningStep.id}")
    learningStep.copy(revision = Some(newRevision))
  }

  def deletePath(learningPathId: Long)(implicit session: DBSession = dbUtility.autoSession): Int = {
    tsql"delete from learningpaths where id = $learningPathId".update().get
  }

  def deleteStep(learningStepId: Long)(implicit session: DBSession = dbUtility.autoSession): Int = {
    tsql"delete from learningsteps where id = $learningStepId".update().get
  }

  def deleteAllPathsAndSteps(implicit session: DBSession): Try[Unit] = for {
    _ <- tsql"delete from learningsteps".update()
    _ <- tsql"delete from learningpaths".update()
  } yield ()

  def learningPathsWithIdBetween(min: Long, max: Long)(implicit
      session: DBSession = dbUtility.readOnlySession
  ): List[LearningPath] = {
    val lp     = DBLearningPath.syntax("lp")
    val status = LearningPathStatus.PUBLISHED.toString

    tsql"""select ${lp.result.*}
               from ${DBLearningPath.as(lp)}
               where lp.document->>'status' = $status
               and lp.id between $min and $max
        """.map(DBLearningPath.fromResultSet(lp.resultName)).runList().get
  }

  def minMaxId(implicit session: DBSession = dbUtility.readOnlySession): (Long, Long) = {
    tsql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from learningpaths"
      .map(rs => (rs.long("mi"), rs.long("ma")))
      .runSingle()
      .get
      .getOrElse((0L, 0L))
  }

  def allPublishedTags(implicit session: DBSession = dbUtility.readOnlySession): List[Tag] = {
    val allTags =
      tsql"""select document->>'tags' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}"""
        .map(rs => {
          rs.string(1)
        })
        .runList()
        .get

    allTags
      .flatMap(tag => {
        CirceUtil.unsafeParseAs[List[Tag]](tag)
      })
      .groupBy(_.language)
      .map(entry => Tag(entry._2.flatMap(_.tags).distinct.sorted, entry._1))
      .toList
  }

  def allPublishedContributors(implicit session: DBSession = dbUtility.readOnlySession): List[Author] = {
    val allCopyrights =
      tsql"""select document->>'copyright' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}"""
        .map(rs => {
          rs.string(1)
        })
        .runList()
        .get

    allCopyrights
      .map(copyright => {
        CirceUtil.unsafeParseAs[LearningpathCopyright](copyright)
      })
      .flatMap(_.contributors)
      .distinct
      .sortBy(_.name)
  }

  private def learningPathsWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where $whereClause"
      .map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps)
      .runList()
      .get
  }

  private def learningPathsWhereWithInactive(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where $whereClause"
      .map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs))
      .runList()
      .get
  }

  private def learningPathWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = dbUtility.readOnlySession): Option[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where $whereClause"
      .map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps)
      .runSingle()
      .get
  }

  def pageWithIds(ids: Seq[Long], pageSize: Int, offset: Int)(implicit
      session: DBSession = dbUtility.readOnlySession
  ): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"""
            select ${lp.resultAll}
            from ${DBLearningPath.as(lp)}
            where ${lp.c("id")} in ($ids)
            order by ${lp.id}
            limit $pageSize
            offset $offset
      """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).runList().get
  }

  def getAllLearningPathsByPage(pageSize: Int, offset: Int)(implicit
      session: DBSession = dbUtility.readOnlySession
  ): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"""
            select ${lp.resultAll}, ${lp.id} as row_id
            from ${DBLearningPath.as(lp)}
            order by row_id
            limit $pageSize
            offset $offset
      """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).runList().get
  }

  def getExternalLinkStepSamples()(implicit session: DBSession = dbUtility.readOnlySession): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"""
      WITH candidates AS (
          SELECT DISTINCT clp.id
          FROM learningpaths clp
          WHERE
            clp."document"->>'isMyNDLAOwner' = 'true'
            AND clp."document"->>'status' = 'UNLISTED'
            AND EXISTS (
              SELECT 1
              FROM jsonb_array_elements(clp.document->'learningsteps') AS step
              WHERE step->>'status' = 'ACTIVE'
                AND jsonb_array_length(step->'embedUrl') > 0
            )
      ),
      matched_ids AS (
          SELECT id
          FROM candidates
          ORDER BY random()
          LIMIT 5
      )
      SELECT ${lp.result.*}
      FROM matched_ids ids
      JOIN ${DBLearningPath.as(lp)} ON ${lp.id} = ids.id
    """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).runList().get

  }

  def getPublishedLearningPathByPage(pageSize: Int, offset: Int)(implicit
      session: DBSession = dbUtility.readOnlySession
  ): List[LearningPath] = {
    val lp  = DBLearningPath.syntax("lp")
    val lps = SubQuery.syntax("lps").include(lp)
    tsql"""
            select ${lps.resultAll} from (select ${lp.resultAll}, ${lp.id} as row_id
                                          from ${DBLearningPath.as(lp)}
                                          where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}
                                          order by ${lp.id}
                                          limit $pageSize
                                          offset $offset) lps
            order by row_id
      """.map(rs => DBLearningPath.fromResultSet(lps(lp).resultName)(rs).withOnlyActiveSteps).runList().get
  }

  def learningPathsWithStatus(
      status: LearningPathStatus
  )(implicit session: DBSession = dbUtility.readOnlySession): List[LearningPath] = {
    learningPathsWhere(sqls"lp.document#>>'{status}' = ${status.toString}")
  }

  def publishedLearningPathCount(implicit session: DBSession = dbUtility.readOnlySession): Long = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select count(*) from ${DBLearningPath.as(lp)} where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}"
      .map(rs => rs.long("count"))
      .runSingle()
      .get
      .getOrElse(0)
  }

  def learningPathCount(implicit session: DBSession = dbUtility.readOnlySession): Long = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select count(*) from ${DBLearningPath.as(lp)}".map(rs => rs.long("count")).runSingle().get.getOrElse(0)
  }

  def myNdlaLearningPathCount(implicit session: DBSession = dbUtility.readOnlySession): Long = {
    val lp = DBLearningPath.syntax("lp")
    tsql"""
           select count(*) from ${DBLearningPath.as(lp)}
           where document@>'{"isMyNDLAOwner": true}' and document->>'status' != ${LearningPathStatus.DELETED.toString}
         """.map(rs => rs.long("count")).runSingle().get.getOrElse(0)
  }

  def myNdlaLearningPathOwnerCount(implicit session: DBSession = dbUtility.readOnlySession): Long = {
    val lp = DBLearningPath.syntax("lp")
    tsql"""
           select count(distinct document ->> 'owner') from ${DBLearningPath.as(lp)}
           where document@>'{"isMyNDLAOwner": true}' and document->>'status' != ${LearningPathStatus.DELETED.toString}
         """.map(rs => rs.long("count")).runSingle().get.getOrElse(0)
  }

  def generateStepId(implicit session: DBSession): Long = {
    sql"select nextval('learningsteps_id_seq')"
      .map(rs => rs.long(1))
      .single()
      .getOrElse(throw new RuntimeException("Could not generate learning step id."))
  }

  private def ensureStepIdSequenceAtLeast(stepId: Long)(implicit session: DBSession): Unit = {
    val _ = sql"""
                 select setval(
                   'learningsteps_id_seq',
                   greatest((select last_value from learningsteps_id_seq), $stepId),
                   true
                 )
               """
      .map(rs => rs.long(1))
      .single()
      .getOrElse(throw new RuntimeException("Could not advance learning step id sequence."))
    ()
  }

  def generateLearningPathId(implicit session: DBSession): Long = {
    sql"select nextval('learningpaths_id_seq')"
      .map(rs => rs.long(1))
      .single()
      .getOrElse(throw new RuntimeException("Could not generate learning path id."))
  }

  private def withStepIdsForInsert(learningpath: LearningPath, learningPathId: Long, startRevision: Int)(implicit
      session: DBSession
  ): LearningPath = {
    val updatedSteps = learningpath
      .learningsteps
      .map { step =>
        val stepId = step.id match {
          case Some(importedId) =>
            ensureStepIdSequenceAtLeast(importedId)
            importedId
          case None => generateLearningPathId
        }
        val stepRevision = step.revision.orElse(Some(startRevision))
        step.copy(id = Some(stepId), revision = stepRevision, learningPathId = Some(learningPathId))
      }
    learningpath.copy(id = Some(learningPathId), revision = Some(startRevision), learningsteps = updatedSteps)
  }

  def withIdWithInactiveSteps(id: Long, includeDeleted: Boolean = false)(implicit
      session: DBSession = AutoSession
  ): Option[LearningPath] = {
    if (includeDeleted) {
      learningPathWhereWithInactiveSteps(sqls"lp.id = $id")
    } else {
      learningPathWhereWithInactiveSteps(
        sqls"lp.id = $id AND lp.document->>'status' <> ${LearningPathStatus.DELETED.toString}"
      )
    }
  }

  private[repository] def learningPathWhereWithInactiveSteps(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Option[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    tsql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where $whereClause"
      .map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs))
      .runSingle()
      .get
  }
}
