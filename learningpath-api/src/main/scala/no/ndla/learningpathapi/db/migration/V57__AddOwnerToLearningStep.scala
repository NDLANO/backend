/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.Json
import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.db.util.*

class V57__AddOwnerToLearningStep extends LearningPathAndStepMigration {
  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow],
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val learningpath = CirceUtil.unsafeParse(lpData.learningPathDocument)
    val owner        = learningpath.hcursor.get[String]("owner") match {
      case Left(value)  => throw new RuntimeException("Parsing learningpath failed")
      case Right(value) => value
    }
    val updatedSteps = stepDatas.map { step =>
      val oldDocument = CirceUtil.unsafeParse(step.learningStepDocument)
      val newDocument = oldDocument.mapObject(_.add("owner", Json.fromString(owner))).noSpaces
      step.copy(learningStepDocument = newDocument)
    }

    (lpData, updatedSteps)
  }
}
