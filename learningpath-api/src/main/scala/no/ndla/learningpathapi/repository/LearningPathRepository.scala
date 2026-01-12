/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.{Author, Tag}
import no.ndla.common.model.domain.learningpath.{
  LearningPath,
  LearningPathStatus,
  LearningStep,
  LearningpathCopyright,
  StepStatus,
}
import no.ndla.database.TrySql.tsql
import no.ndla.learningpathapi.model.domain.*
import org.postgresql.util.PGobject
import scalikejdbc.*

import java.util.UUID
import scala.util.Try

class LearningPathRepository extends StrictLogging {

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A = {
    Option(session) match {
      case Some(x) => work(x)
      case None    => DB localTx { implicit newSession =>
          work(newSession)
        }
    }
  }

  def withId(id: Long)(implicit session: DBSession = AutoSession): Option[LearningPath] = {
    learningPathWhere(sqls"lp.id = $id AND lp.document->>'status' <> ${LearningPathStatus.DELETED.toString}")
  }

  def withIdIncludingDeleted(id: Long)(implicit session: DBSession = AutoSession): Option[LearningPath] = {
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

  def getIdFromExternalId(externalId: String)(implicit session: DBSession = AutoSession): Option[Long] = {
    sql"select id from learningpaths where external_id = $externalId".map(rs => rs.long("id")).single()
  }

  def learningPathsWithIsBasedOn(isBasedOnId: Long): List[LearningPath] = {
    learningPathsWhere(sqls"lp.document->>'isBasedOn' = ${isBasedOnId.toString}")
  }

  def learningStepsFor(learningPathId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[LearningStep] = {
    withId(learningPathId).map(_.learningsteps).getOrElse(Seq.empty)
  }

  def learningStepWithId(learningPathId: Long, learningStepId: Long)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): Option[LearningStep] = {
    val ls = DBLearningStep.syntax("ls")
    sql"select ${ls.result.*} from ${DBLearningStep.as(ls)} where ${ls.learningPathId} = $learningPathId and ${ls.id} = $learningStepId"
      .map(DBLearningStep.fromResultSet(ls.resultName))
      .single()
  }

  def learningStepWithExternalIdAndForLearningPath(externalId: Option[String], learningPathId: Option[Long])(implicit
      session: DBSession = ReadOnlyAutoSession
  ): Option[LearningStep] = {
    if (externalId.isEmpty || learningPathId.isEmpty) {
      None
    } else {
      val ls = DBLearningStep.syntax("ls")
      sql"select ${ls.result.*} from ${DBLearningStep.as(ls)} where ${ls.externalId} = ${externalId.get} and ${ls.learningPathId} = ${learningPathId.get}"
        .map(DBLearningStep.fromResultSet(ls.resultName))
        .single()
    }
  }

  // TODO: Implement ID generation logic, maybe keep using the sequence of the primary key of the table we are about to delete?
  private def generateStepId(implicit session: DBSession): Long = ???

  def addStepIds(learningPath: LearningPath)(implicit session: DBSession): LearningPath =
    learningPath.copy(learningsteps =
      learningPath
        .learningsteps
        .map { ls =>
          val newStepId = generateStepId(using session)
          ls.copy(id = Some(newStepId), learningPathId = learningPath.id)
        }
    )

  def insert(learningpath: LearningPath)(implicit session: DBSession = AutoSession): Try[LearningPath] = {
    val startRevision = 1
    val dataObject    = new PGobject()
    dataObject.setType("jsonb")

    val pathToInsert = addStepIds(learningpath)
    dataObject.setValue(CirceUtil.toJsonString(learningpath))

    val learningPathId: Long =
      tsql"insert into learningpaths(external_id, document, revision) values(${learningpath.externalId}, $dataObject, $startRevision)"
        .updateAndReturnGeneratedKey()

    logger.info(s"Inserted learningpath with id $learningPathId")
    pathToInsert.copy(id = Some(learningPathId), revision = Some(startRevision))
  }

  def insertWithImportId(learningpath: LearningPath, importId: String)(implicit
      session: DBSession = AutoSession
  ): LearningPath = {
    val startRevision = 1
    val dataObject    = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(learningpath))

    val importIdUUID         = Try(UUID.fromString(importId)).toOption
    val learningPathId: Long =
      sql"insert into learningpaths(external_id, document, revision, import_id) values(${learningpath.externalId}, $dataObject, $startRevision, $importIdUUID)"
        .updateAndReturnGeneratedKey()
    val learningSteps = learningpath
      .learningsteps
      .map(lsteps =>
        lsteps.map(learningStep => {
          insertLearningStep(learningStep.copy(learningPathId = Some(learningPathId)))
        })
      )

    logger.info(s"Inserted learningpath with id $learningPathId")
    learningpath.copy(id = Some(learningPathId), revision = Some(startRevision), learningsteps = learningSteps)
  }

  def idAndimportIdOfLearningpath(
      externalId: String
  )(implicit session: DBSession = AutoSession): Option[(Long, Option[String])] = {
    val lp = DBLearningPath.syntax("lp")
    sql"""select id, import_id
            from ${DBLearningPath.as(lp)}
            where lp.document is not NULL and lp.external_id = $externalId"""
      .map(rs => (rs.long("id"), rs.stringOpt("import_id")))
      .single()
  }

  def update(learningpath: LearningPath)(implicit session: DBSession = AutoSession): LearningPath = {
    if (learningpath.id.isEmpty) {
      throw new RuntimeException("A non-persisted learningpath cannot be updated without being saved first.")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(CirceUtil.toJsonString(learningpath))

    val newRevision = learningpath.revision.getOrElse(0) + 1
    val count       =
      sql"update learningpaths set document = $dataObject, revision = $newRevision where id = ${learningpath.id} and revision = ${learningpath.revision}"
        .update()

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
      session: DBSession = AutoSession
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
      sql"update learningpaths set document = $dataObject, revision = $newRevision, import_id = $importIdUUID where id = ${learningpath.id} and revision = ${learningpath.revision}"
        .update()

    if (count != 1) {
      val msg =
        s"Conflicting revision is detected for learningPath with id = ${learningpath.id} and revision = ${learningpath.revision}"
      logger.warn(msg)
      throw new OptimisticLockException(msg)
    }

    logger.info(s"Updated learningpath with id ${learningpath.id}")
    learningpath.copy(revision = Some(newRevision))
  }

  def deletePath(learningPathId: Long)(implicit session: DBSession = AutoSession): Int = {
    sql"delete from learningpaths where id = $learningPathId".update()
  }

  def deleteAllPathsAndSteps(implicit session: DBSession): Try[Unit] = for {
    _ <- Try(sql"delete from learningpaths".update())
  } yield ()

  def learningPathsWithIdBetween(min: Long, max: Long)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): List[LearningPath] = {
    val (lp, ls) = (DBLearningPath.syntax("lp"), DBLearningStep.syntax("ls"))
    val status   = LearningPathStatus.PUBLISHED.toString

    sql"""select ${lp.result.*}, ${ls.result.*}
               from ${DBLearningPath.as(lp)}
               where lp.document->>'status' = $status
               and lp.id between $min and $max""".map(DBLearningPath.fromResultSet(lp.resultName)).list()
  }

  def minMaxId(implicit session: DBSession = ReadOnlyAutoSession): (Long, Long) = {
    sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from learningpaths"
      .map(rs => {
        (rs.long("mi"), rs.long("ma"))
      })
      .single() match {
      case Some(minmax) => minmax
      case None         => (0L, 0L)
    }
  }

  def allPublishedTags(implicit session: DBSession = ReadOnlyAutoSession): List[Tag] = {
    val allTags =
      sql"""select document->>'tags' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}"""
        .map(rs => {
          rs.string(1)
        })
        .list()

    allTags
      .flatMap(tag => {
        CirceUtil.unsafeParseAs[List[Tag]](tag)
      })
      .groupBy(_.language)
      .map(entry => Tag(entry._2.flatMap(_.tags).distinct.sorted, entry._1))
      .toList
  }

  def allPublishedContributors(implicit session: DBSession = ReadOnlyAutoSession): List[Author] = {
    val allCopyrights =
      sql"""select document->>'copyright' from learningpaths where document->>'status' = ${LearningPathStatus.PUBLISHED.toString}"""
        .map(rs => {
          rs.string(1)
        })
        .list()

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
  )(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    sql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where $whereClause"
      .map { rs =>
        val learningpath = DBLearningPath.fromResultSet(lp.resultName)(rs)
        learningpath.withOnlyActiveSteps
      }
      .list()
  }

  private def learningPathWhere(
      whereClause: SQLSyntax
  )(implicit session: DBSession = ReadOnlyAutoSession): Option[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    sql"select ${lp.result.*} from ${DBLearningPath.as(lp)} where $whereClause"
      .map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps)
      .single()
  }

  def pageWithIds(ids: Seq[Long], pageSize: Int, offset: Int)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): List[LearningPath] = {
    val lp  = DBLearningPath.syntax("lp")
    val lps = SubQuery.syntax("lps").include(lp)
    sql"""
            select ${lps.resultAll} from (select ${lp.resultAll}
                                          from ${DBLearningPath.as(lp)}
                                          where ${lp.c("id")} in ($ids)
                                          limit $pageSize
                                          offset $offset) lps
      """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).list()
  }

  def getAllLearningPathsByPage(pageSize: Int, offset: Int)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): List[LearningPath] = {
    val lp  = DBLearningPath.syntax("lp")
    val lps = SubQuery.syntax("lps").include(lp)
    sql"""
            select ${lps.resultAll} from (select ${lp.resultAll}, ${lp.id} as row_id
                                          from ${DBLearningPath.as(lp)}
                                          limit $pageSize
                                          offset $offset) lps
            order by row_id
      """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).list()
  }

  def getExternalLinkStepSamples()(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
    val lp = DBLearningPath.syntax("lp")
    sql"""
      WITH candidates AS (
          SELECT DISTINCT clp.id
          FROM learningpaths clp
          WHERE
            clp."document"->>'isMyNDLAOwner' = 'true'
            AND clp."document"->>'status' = 'UNLISTED'
            AND EXISTS (
              SELECT 1
              FROM learningsteps lss
              WHERE lss.learning_path_id = clp.id
                AND jsonb_array_length(lss."document"->'embedUrl') > 0
                AND lss."document"->>'status' = 'ACTIVE'
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
    """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).list()

  }

  def getPublishedLearningPathByPage(pageSize: Int, offset: Int)(implicit
      session: DBSession = ReadOnlyAutoSession
  ): List[LearningPath] = {
    val lp  = DBLearningPath.syntax("lp")
    val lps = SubQuery.syntax("lps").include(lp)
    sql"""
            select ${lps.resultAll} from (select ${lp.resultAll}, ${lp.id} as row_id
                                          from ${DBLearningPath.as(lp)}
                                          where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}
                                          limit $pageSize
                                          offset $offset) lps
            order by row_id
      """.map(rs => DBLearningPath.fromResultSet(lp.resultName)(rs).withOnlyActiveSteps).list()
  }

  def learningPathsWithStatus(
      status: LearningPathStatus
  )(implicit session: DBSession = ReadOnlyAutoSession): List[LearningPath] = {
    learningPathsWhere(sqls"lp.document#>>'{status}' = ${status.toString}")
  }

  def publishedLearningPathCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
    val (lp, _) = (DBLearningPath.syntax("lp"), DBLearningStep.syntax("ls"))
    sql"select count(*) from ${DBLearningPath.as(lp)} where document#>>'{status}' = ${LearningPathStatus.PUBLISHED.toString}"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def learningPathCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
    val (lp, _) = (DBLearningPath.syntax("lp"), DBLearningStep.syntax("ls"))
    sql"select count(*) from ${DBLearningPath.as(lp)}".map(rs => rs.long("count")).single().getOrElse(0)
  }

  def myNdlaLearningPathCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
    val lp = DBLearningPath.syntax("lp")
    sql"""
           select count(*) from ${DBLearningPath.as(lp)}
           where document@>'{"isMyNDLAOwner": true}' and document->>'status' != ${LearningPathStatus.DELETED.toString}
         """.map(rs => rs.long("count")).single().getOrElse(0)
  }

  def myNdlaLearningPathOwnerCount(implicit session: DBSession = ReadOnlyAutoSession): Long = {
    val lp = DBLearningPath.syntax("lp")
    sql"""
           select count(distinct document ->> 'owner') from ${DBLearningPath.as(lp)}
           where document@>'{"isMyNDLAOwner": true}' and document->>'status' != ${LearningPathStatus.DELETED.toString}
         """.map(rs => rs.long("count")).single().getOrElse(0)
  }
}
