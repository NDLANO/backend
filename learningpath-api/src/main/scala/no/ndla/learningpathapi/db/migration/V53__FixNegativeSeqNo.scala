/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.learningpathapi.db.util.LearningPathAndStepMigration
import no.ndla.learningpathapi.db.util.LpDocumentRow
import no.ndla.learningpathapi.db.util.StepDocumentRow
import no.ndla.common.CirceUtil
import io.circe.Encoder
import io.circe.syntax.EncoderOps

class V53__FixNegativeSeqNo extends LearningPathAndStepMigration {
  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow]
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val sortedSteps = stepDatas.sortBy(step => {
      val stepDocument = CirceUtil.tryParse(step.learningStepDocument).get
      val seqNo        = stepDocument.hcursor.get[Option[Long]]("seqNo").toTry.get.get
      seqNo
    })

    (lpData, convertSteps(sortedSteps))
  }

  private def convertSteps(sortedSteps: List[StepDocumentRow])(using encoder: Encoder[Int]): List[StepDocumentRow] = {
    sortedSteps.zipWithIndex.map((step, i) => {
      val stepDocument = CirceUtil.tryParse(step.learningStepDocument).get
      val newData      = stepDocument.mapObject(doc => doc.remove("seqNo").add("seqNo", i.asJson))
      step.copy(learningStepDocument = newData.noSpaces)
    })
  }
}
