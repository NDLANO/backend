/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.util

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.postgresql.util.PGobject
import scalikejdbc.*

case class LpDocumentRow(
    learningPathId: Long,
    learningPathDocument: String
)

case class StepDocumentRow(
    learningStepId: Long,
    learningStepDocument: String
)

abstract class LearningPathAndStepMigration extends BaseJavaMigration {
  def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow]
  ): (LpDocumentRow, List[StepDocumentRow])
  val chunkSize: Int = 1000

  override def migrate(context: Context): Unit = DB(context.getConnection)
    .autoClose(false)
    .withinTx { session => migrateRows(using session) }

  private def countAllRows(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from learningpaths where document is not null"
      .map(rs => rs.long("count"))
      .single()
  }

  private def allLearningPaths(offset: Long)(implicit session: DBSession): List[LpDocumentRow] = {
    sql"select id, document from learningpaths where document is not null order by id limit 1000 offset $offset"
      .map(rs => LpDocumentRow(rs.long("id"), rs.string("document")))
      .list()
  }

  private def getStepDatas(learningPathId: Long)(using session: DBSession): List[StepDocumentRow] = {
    sql"select id, document from learningsteps where learning_path_id = $learningPathId and document is not null order by id"
      .map(rs => StepDocumentRow(rs.long("id"), rs.string("document")))
      .list()
  }

  private def updateLp(existingRow: LpDocumentRow, newRow: LpDocumentRow)(using session: DBSession): Unit = {
    if (existingRow == newRow) return
    if (existingRow.learningPathId != newRow.learningPathId) {
      throw new RuntimeException(s"Learning path id mismatch: $existingRow -> $newRow")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(newRow.learningPathDocument)
    val updated = sql"update learningpaths set document = $dataObject where id = ${newRow.learningPathId}".update()
    if (updated != 1) throw new RuntimeException(s"Failed to update learning path document $existingRow -> $newRow")
  }

  private def updateStep(existingStep: StepDocumentRow, newStep: StepDocumentRow)(using session: DBSession): Unit = {
    if (existingStep == newStep) return
    if (existingStep.learningStepId != newStep.learningStepId) {
      throw new RuntimeException(s"Cannot update learning step with different IDs: $existingStep -> $newStep")
    }

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(newStep.learningStepDocument)
    val updated = sql"update learningsteps set document = $dataObject where id = ${newStep.learningStepId}".update()
    if (updated != 1) throw new RuntimeException(s"Failed to update learning step document $existingStep -> $newStep")
  }

  private def updateRow(path: LpDocumentRow, steps: List[StepDocumentRow])(using session: DBSession): Unit = {
    val (newPath, newSteps) = convertPathAndSteps(path, steps)
    updateLp(path, newPath)(using session)
    newSteps.foreach { step =>
      val existingStep = steps.find(_.learningStepId == step.learningStepId)
      existingStep match {
        case Some(existing) => updateStep(existing, step)(using session)
        case None => throw new RuntimeException(s"Step with id ${step.learningStepId} not found in existing steps")
      }
    }
  }

  private def migrateRows(using session: DBSession): Unit = {
    val count        = countAllRows.get
    var numPagesLeft = (count / chunkSize) + 1
    var offset       = 0L

    while (numPagesLeft > 0) {
      allLearningPaths(offset * chunkSize).map { lpData =>
        val stepDatas = getStepDatas(lpData.learningPathId)(using session)
        updateRow(lpData, stepDatas)
      }: Unit
      numPagesLeft -= 1
      offset += 1
    }
  }
}
