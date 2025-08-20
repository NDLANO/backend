/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import no.ndla.common.CirceUtil
import no.ndla.learningpathapi.db.util.{LearningPathAndStepMigration, LpDocumentRow, StepDocumentRow}

class V48__FixLanguageMyNDLALearningPath extends LearningPathAndStepMigration {

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

    val title = oldDocument.hcursor.downField("title").as[Option[Seq[Title]]].toTry.get.filterOtherLanguagesOrEmpty
    val description =
      oldDocument.hcursor.downField("description").as[Option[Seq[Description]]].toTry.get.filterOtherLanguagesOrEmpty
    val tags = oldDocument.hcursor.downField("tags").as[Option[Seq[Tag]]].toTry.get.filterOtherLanguagesOrEmpty

    val newDocument = oldDocument
      .mapObject(doc =>
        doc
          .remove("title")
          .add("title", title.asJson)
          .remove("description")
          .add("description", description.asJson)
          .remove("tags")
          .add("tags", tags.asJson)
      )
      .noSpaces

    pathData.copy(learningPathDocument = newDocument)
  }

  private def convertStep(stepData: StepDocumentRow): StepDocumentRow = {
    val oldDocument = CirceUtil.tryParse(stepData.learningStepDocument).get

    val title = oldDocument.hcursor.downField("title").as[Option[Seq[Title]]].toTry.get.filterOtherLanguagesOrEmpty
    val description =
      oldDocument.hcursor.downField("description").as[Option[Seq[Description]]].toTry.get.filterOtherLanguagesOrEmpty
    val introduction =
      oldDocument.hcursor.downField("introduction").as[Option[Seq[Introduction]]].toTry.get.filterOtherLanguagesOrEmpty
    val embedUrl =
      oldDocument.hcursor.downField("embedUrl").as[Option[Seq[EmbedUrl]]].toTry.get.filterOtherLanguagesOrEmpty

    val newDocument = oldDocument
      .mapObject(doc =>
        doc
          .remove("title")
          .add("title", title.asJson)
          .remove("description")
          .add("description", description.asJson)
          .remove("introduction")
          .add("introduction", introduction.asJson)
          .remove("embedUrl")
          .add("embedUrl", embedUrl.asJson)
      )
      .noSpaces

    stepData.copy(learningStepDocument = newDocument)
  }

  extension [T <: LanguageField](values: Option[Seq[T]]) {
    private def filterOtherLanguagesOrEmpty: Seq[T] = {
      values match {
        case Some(values) if values.size > 1 => values.filter(_.language == "nb")
        case Some(values)                    => values
        case None                            => Seq.empty
      }
    }
  }
}
