/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Encoder, JsonObject}
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.db.util.{LearningPathAndStepMigration, LpDocumentRow, StepDocumentRow}

class V49__FixLanguageMyNDLALearningPath extends LearningPathAndStepMigration, StrictLogging {

  private class LanguageField(val language: String)
  private case class Title(title: String, override val language: String)               extends LanguageField(language)
  private case class Description(description: String, override val language: String)   extends LanguageField(language)
  private case class Tag(tags: Seq[String], override val language: String)             extends LanguageField(language)
  private case class Introduction(introduction: String, override val language: String) extends LanguageField(language)
  private case class EmbedUrl(url: String, embedType: String, override val language: String)
      extends LanguageField(language)

  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow]
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val lpDocument = CirceUtil.tryParse(lpData.learningPathDocument).get
    if (!lpDocument.hcursor.downField("isMyNDLAOwner").as[Boolean].toTry.get) {
      return (lpData, stepDatas)
    }

    val newLpData    = convertPath(lpData)
    val newStepDatas = stepDatas.map(convertStep)
    (newLpData, newStepDatas)
  }

  private def convertPath(pathData: LpDocumentRow): LpDocumentRow = {
    val oldDocument = CirceUtil.tryParse(pathData.learningPathDocument).get

    val title       = oldDocument.hcursor.get[Option[Seq[Title]]]("title").toTry.get.filterOtherLanguages
    val description = oldDocument.hcursor.get[Option[Seq[Description]]]("description").toTry.get.filterOtherLanguages
    val tags        = oldDocument.hcursor.get[Option[Seq[Tag]]]("tags").toTry.get.filterOtherLanguages

    val newDocument = oldDocument
      .mapObject(doc =>
        doc
          .maybeChange("title", title)
          .maybeChange("description", description)
          .maybeChange("tags", tags)
      )

    if (newDocument != oldDocument) {
      logger.warn(s"Removing non-NB languages from learning path with ID ${pathData.learningPathId}")
    }

    pathData.copy(learningPathDocument = newDocument.noSpaces)
  }

  private def convertStep(stepData: StepDocumentRow): StepDocumentRow = {
    val oldDocument = CirceUtil.tryParse(stepData.learningStepDocument).get

    val title        = oldDocument.hcursor.get[Option[Seq[Title]]]("title").toTry.get.filterOtherLanguages
    val description  = oldDocument.hcursor.get[Option[Seq[Description]]]("description").toTry.get.filterOtherLanguages
    val introduction = oldDocument.hcursor.get[Option[Seq[Introduction]]]("introduction").toTry.get.filterOtherLanguages
    val embedUrl     = oldDocument.hcursor.get[Option[Seq[EmbedUrl]]]("embedUrl").toTry.get.filterOtherLanguages

    val newDocument = oldDocument
      .mapObject(doc =>
        doc
          .maybeChange("title", title)
          .maybeChange("description", description)
          .maybeChange("introduction", introduction)
          .maybeChange("embedUrl", embedUrl)
      )

    if (newDocument != oldDocument) {
      logger.warn(s"Removing non-NB languages from learning step with ID ${stepData.learningStepId}")
    }

    stepData.copy(learningStepDocument = newDocument.noSpaces)
  }

  extension [T <: LanguageField](values: Option[Seq[T]]) {
    private def filterOtherLanguages: Option[Seq[T]] = {
      values match {
        case Some(values) if values.size > 1 => Some(values.filter(_.language == "nb"))
        case Some(values)                    => None
        case None                            => None
      }
    }
  }

  extension (jsonObj: JsonObject) {
    private def maybeChange[T <: LanguageField](key: String, value: Option[Seq[T]])(using
        encoder: Encoder[Seq[T]]
    ): JsonObject = value match {
      case Some(value) => jsonObj.remove(key).add(key, value.asJson)
      case None        => jsonObj
    }
  }
}
