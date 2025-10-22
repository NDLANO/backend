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

class V48__AddContributorsToLearningStep extends LearningPathAndStepMigration {
  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow],
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val updatedSteps = stepDatas.map { step =>
      val json = CirceUtil.unsafeParse(step.learningStepDocument)
      moveLicenseToCopyright(json) match {
        case Some(updated) => step.copy(learningStepDocument = updated.noSpaces)
        case None          => step
      }
    }

    (lpData, updatedSteps)
  }

  private def moveLicenseToCopyright(json: Json): Option[Json] = {
    val changed = json.hcursor.get[Option[String]]("license") match {
      case Left(value)        => throw new RuntimeException("Parsing learningstep failed")
      case Right(Some(value)) =>
        val newJson         = json.mapObject(_.remove("license"))
        val copyrightObject = Json.obj("license" -> Json.fromString(value), "contributors" -> Json.arr())
        newJson.mapObject(obj => obj.remove("license").add("copyright", copyrightObject))
      case Right(None) => json.mapObject(_.remove("license"))
    }

    Option.when(changed != json) {
      changed
    }
  }
}
