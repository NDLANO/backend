/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.{EmbedUrl, StepType}
import no.ndla.learningpathapi.db.util.*
import io.circe.syntax.EncoderOps

class V62__ConvertLearningStepType extends LearningPathAndStepMigration {

  private[migration] def convertStep(document: String): String = {
    val step      = CirceUtil.tryParse(document).get
    val embedUrl  = step.hcursor.get[Option[Seq[EmbedUrl]]]("embedUrl").toTry.get.get
    val articleId = step.hcursor.get[Option[Int]]("articleId").toTry.get
    if (articleId.isDefined) {
      return step
        .mapObject(doc =>
          doc
            .remove("type")
            .add("type", StepType.ARTICLE.entryName.asJson)
            .remove("embedUrl")
            .add("embedUrl", Seq.empty[EmbedUrl].asJson)
        )
        .noSpaces
    }

    val newStepType =
      if (embedUrl.exists(url => url.url.nonEmpty)) StepType.EXTERNAL
      else StepType.TEXT

    step
      .mapObject(doc =>
        doc
          .remove("type")
          .add("type", newStepType.entryName.asJson)
          .remove("embedUrl")
          .add("embedUrl", embedUrl.filterNot(_.url.isEmpty).asJson)
      )
      .noSpaces
  }

  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow],
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val updatedSteps = stepDatas.map(step => step.copy(learningStepDocument = convertStep(step.learningStepDocument)))

    (lpData, updatedSteps)
  }

}
