/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import com.typesafe.scalalogging.StrictLogging
import io.circe.Json
import no.ndla.common.CirceUtil
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import org.slf4j.MDC
import scalikejdbc.*

case class LpDocumentRowWithId(learningPathId: Long, learningPathDocument: String)
case class StepDocumentRowWithMeta(
    learningStepId: Long,
    learningPathId: Long,
    revision: Int,
    externalId: Option[String],
    learningStepDocument: String,
)

class V60__MoveLearningStepsToLearningPathDocument extends BaseJavaMigration with StrictLogging {
  private val chunkSize = 1000

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session =>
      migrateRows(using session)
    }

  private def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from learningpaths where document is not null".map(rs => rs.long("count")).single()
  }

  private def allLearningPaths(offset: Long)(implicit session: DBSession): List[LpDocumentRowWithId] = {
    sql"select id, document from learningpaths where document is not null order by id limit $chunkSize offset $offset"
      .map(rs => LpDocumentRowWithId(rs.long("id"), rs.string("document")))
      .list()
  }

  private def getStepDatas(learningPathId: Long)(using session: DBSession): List[StepDocumentRowWithMeta] = {
    sql"""
      select id, learning_path_id, revision, external_id, document
      from learningsteps
      where learning_path_id = $learningPathId and document is not null
      order by id
    """
      .map { rs =>
        StepDocumentRowWithMeta(
          learningStepId = rs.long("id"),
          learningPathId = rs.long("learning_path_id"),
          revision = rs.int("revision"),
          externalId = rs.stringOpt("external_id"),
          learningStepDocument = rs.string("document"),
        )
      }
      .list()
  }

  private def updateLp(
      row: LpDocumentRowWithId,
      steps: List[StepDocumentRowWithMeta],
  )(using session: DBSession): Unit = {
    val updatedLpJson = CirceUtil.tryParse(mergeLearningSteps(row.learningPathDocument, steps)).get

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(updatedLpJson.noSpaces)

    val updated = sql"update learningpaths set document = $dataObject where id = ${row.learningPathId}".update()
    if (updated != 1)
      throw new RuntimeException(s"Failed to update learning path document for id ${row.learningPathId}")
  }

  private[migration] def mergeLearningSteps(
      learningPathDocument: String,
      steps: List[StepDocumentRowWithMeta],
  ): String = {
    val oldLp        = CirceUtil.tryParse(learningPathDocument).get
    val updatedSteps = steps.sortBy(stepSeqNo).map(enrichStep)
    oldLp.mapObject(_.remove("learningsteps").add("learningsteps", Json.fromValues(updatedSteps))).noSpaces
  }

  private def enrichStep(step: StepDocumentRowWithMeta): Json = {
    val json = CirceUtil.tryParse(step.learningStepDocument).get
    json.mapObject { obj =>
      val withIds = obj
        .add("id", Json.fromLong(step.learningStepId))
        .add("revision", Json.fromInt(step.revision))
        .add("learningPathId", Json.fromLong(step.learningPathId))
      step.externalId match {
        case Some(value) => withIds.add("externalId", Json.fromString(value))
        case None        => withIds.remove("externalId")
      }
    }
  }

  private def stepSeqNo(step: StepDocumentRowWithMeta): Int = {
    val json = CirceUtil.tryParse(step.learningStepDocument).get
    json.hcursor.get[Int]("seqNo").toOption.getOrElse(Int.MaxValue)
  }

  private def migrateRows(using session: DBSession): Unit = {
    val count        = countAllRows.getOrElse(0L)
    var numPagesLeft = (count / chunkSize) + 1
    var offset       = 0L

    MDC.put("migrationName", this.getClass.getSimpleName): Unit
    while (numPagesLeft > 0) {
      allLearningPaths(offset * chunkSize).foreach { lpData =>
        val steps = getStepDatas(lpData.learningPathId)(using session)
        updateLp(lpData, steps)(using session)
      }
      numPagesLeft -= 1
      offset += 1
    }
    MDC.remove("migrationName"): Unit
  }
}
