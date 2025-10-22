/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.db.migration

import io.circe.syntax.EncoderOps
import no.ndla.learningpathapi.db.util.LearningPathAndStepMigration
import com.typesafe.scalalogging.StrictLogging
import no.ndla.learningpathapi.db.util.LpDocumentRow
import no.ndla.learningpathapi.db.util.StepDocumentRow
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.learningpath.Introduction
import no.ndla.common.model.domain.learningpath.EmbedUrl
import no.ndla.common.model.domain.learningpath.Description
import io.circe.Encoder

class V54__ConvertIntroductionSteps extends LearningPathAndStepMigration, StrictLogging {

  override def convertPathAndSteps(
      lpData: LpDocumentRow,
      stepDatas: List[StepDocumentRow],
  ): (LpDocumentRow, List[StepDocumentRow]) = {
    val sortedSteps = stepDatas.sortBy(step => {
      val doc = CirceUtil.tryParse(step.learningStepDocument).get
      doc.hcursor.get[Option[Long]]("seqNo").toTry.get.get
    })

    val stepToDelete = sortedSteps.find(step => {
      val doc = CirceUtil.tryParse(step.learningStepDocument).get
      doc.hcursor.get[Option[String]]("status").toTry.get.get != "DELETED"
    })

    val introduction = stepToDelete.flatMap(getLpIntroduction)

    (stepToDelete, introduction) match {
      case (Some(toDelete), Some(intro)) => (
          convertPath(lpData, intro),
          convertSteps(stepDatas.filterNot(_.learningStepId == toDelete.learningStepId), toDelete),
        )
      case _ => (lpData, stepDatas)
    }
  }

  private def getLpIntroduction(step: StepDocumentRow): Option[Seq[Introduction]] = {
    val doc           = CirceUtil.tryParse(step.learningStepDocument).get
    val stepType      = doc.hcursor.get[Option[String]]("type").toTry.get.get
    val embedUrl      = doc.hcursor.get[Option[Seq[EmbedUrl]]]("embedUrl").toTry.get.get
    val descriptions  = doc.hcursor.get[Option[Seq[Description]]]("description").toTry.get.get
    val introductions = doc.hcursor.get[Option[Seq[Introduction]]]("introduction").toTry.get.get

    (stepType, embedUrl, introductions) match {
      case ("INTRODUCTION", Nil, Nil) =>
        Some(descriptions.map(desc => Introduction(s"<section>${desc.description}</section>", desc.language)))
      case _ => None
    }
  }

  private def convertSteps(steps: List[StepDocumentRow], deletedStep: StepDocumentRow) = {
    val deletedSeqNo = CirceUtil
      .tryParse(deletedStep.learningStepDocument)
      .get
      .hcursor
      .get[Option[Long]]("seqNo")
      .toTry
      .get
      .get
    steps.map(step => {
      val oldStep = CirceUtil.tryParse(step.learningStepDocument).get
      val oldSeq  = oldStep.hcursor.get[Option[Long]]("seqNo").toTry.get.get
      oldSeq > deletedSeqNo match {
        case false => step
        case true  => step.copy(learningStepDocument =
            oldStep
              .mapObject(doc =>
                doc
                  .remove("seqNo")
                  .add(
                    "seqNo",
                    (
                      oldSeq - 1
                    ).asJson,
                  )
              )
              .noSpaces
          )
      }
    })

  }

  private def convertPath(lpData: LpDocumentRow, introduction: Seq[Introduction])(using
      encoder: Encoder[Seq[Introduction]]
  ): LpDocumentRow = {
    val oldLp   = CirceUtil.tryParse(lpData.learningPathDocument).get
    val newData = oldLp.mapObject(doc => doc.remove("introduction").add("introduction", introduction.asJson))
    lpData.copy(learningPathDocument = newData.noSpaces)
  }
}
