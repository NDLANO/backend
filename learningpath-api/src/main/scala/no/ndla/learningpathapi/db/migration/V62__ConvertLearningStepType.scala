/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.Json
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.{EmbedUrl, StepType}
import io.circe.syntax.EncoderOps
import no.ndla.database.DocumentMigration

class V62__ConvertLearningStepType extends DocumentMigration {
  override val columnName: String = "document"
  override val tableName: String  = "learningpaths"

  private[migration] def convertStep(step: Json): Json = {
    val embedUrl  = step.hcursor.get[Option[Seq[EmbedUrl]]]("embedUrl").toTry.get.get
    val articleId = step.hcursor.get[Option[Int]]("articleId").toTry.get
    if (articleId.isDefined) {
      return step.mapObject(doc =>
        doc
          .remove("type")
          .add("type", StepType.ARTICLE.entryName.asJson)
          .remove("embedUrl")
          .add("embedUrl", Seq.empty[EmbedUrl].asJson)
      )
    }

    val newStepType =
      if (embedUrl.exists(url => url.url.nonEmpty)) StepType.EXTERNAL
      else StepType.TEXT

    step.mapObject(doc =>
      doc
        .remove("type")
        .add("type", newStepType.entryName.asJson)
        .remove("embedUrl")
        .add("embedUrl", embedUrl.filterNot(_.url.isEmpty).asJson)
    )
  }

  override def convertColumn(value: String): String = {
    val document    = CirceUtil.tryParse(value).get
    val newDocument = document.hcursor.downField("learningsteps").withFocus(_.mapArray(_.map(convertStep)))
    newDocument.top.get.noSpaces

  }
}
