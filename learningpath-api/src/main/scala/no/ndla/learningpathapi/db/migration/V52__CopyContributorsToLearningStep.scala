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
import no.ndla.common.model.domain.Author
import no.ndla.common.model.domain.learningpath.LearningpathCopyright
import no.ndla.learningpathapi.db.util.*

class V52__CopyContributorsToLearningStep extends LearningPathAndStepMigration {
  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow]
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val learningPathJson = CirceUtil.unsafeParse(lpData.learningPathDocument)
    val contributorsOpt  = for {
      copyright    <- learningPathJson.hcursor.downField("copyright").as[LearningpathCopyright].toOption
      contributors <- Option(copyright.contributors)
    } yield contributors

    if (learningPathJson.hcursor.downField("isMyNDLAOwner").as[Boolean].getOrElse(false)) {

      // extract contributors from learning path and copy to each step of type TEXT without embedUrl
      val updatedSteps = stepDatas.map { step =>
        val json         = CirceUtil.unsafeParse(step.learningStepDocument)
        val cursor       = json.hcursor
        val isText       = cursor.get[String]("type").toOption.contains("TEXT")
        val hasEmbedUrl  = cursor.get[String]("embedUrl").toOption.isDefined
        val hasArticleId = cursor.get[Long]("articleId").toOption.isDefined

        if (isText && !hasEmbedUrl && !hasArticleId && contributorsOpt.isDefined) {
          addContributorsToCopyright(json, contributorsOpt.get) match {
            case Some(updated) => step.copy(learningStepDocument = updated.noSpaces)
            case None          => step
          }
        } else {
          step
        }
      }
      return (lpData, updatedSteps)
    }

    (lpData, stepDatas)
  }

  private def addContributorsToCopyright(json: Json, contributors: Seq[Author]): Option[Json] = {
    import no.ndla.common.model.domain.learningpath.LearningpathCopyright.encoder
    val changed = json.hcursor.get[Option[LearningpathCopyright]]("copyright") match {
      case Left(value)        => throw new RuntimeException("Parsing learningstep failed")
      case Right(Some(value)) =>
        val copyrightObject = value.copy(contributors = contributors)
        json.mapObject(obj => obj.remove("copyright").add("copyright", encoder.apply(copyrightObject)))
      case Right(None) => json
    }

    Option.when(changed != json) { changed }
  }
}
