/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import no.ndla.searchapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.searchapi.model.domain.Title
import no.ndla.searchapi.model.domain.article.MetaDescription

case class LearningStep(id: Option[Long],
                        revision: Option[Int],
                        externalId: Option[String],
                        learningPathId: Option[Long],
                        seqNo: Int,
                        title: Seq[Title],
                        description: Seq[Description],
                        embedUrl: Seq[EmbedUrl],
                        `type`: StepType.Value,
                        license: Option[String],
                        showTitle: Boolean = false,
                        status: StepStatus.Value = StepStatus.ACTIVE) {}

object StepStatus extends Enumeration {

  val ACTIVE, DELETED = Value

  def valueOf(s: String): Option[StepStatus.Value] = {
    StepStatus.values.find(_.toString == s)
  }

  def valueOfOrError(status: String): StepStatus.Value = {
    valueOf(status) match {
      case Some(s) => s
      case None =>
        throw new ValidationException(errors = List(ValidationMessage("status", s"'$status' is not a valid status.")))
    }
  }

  def valueOfOrDefault(s: String): StepStatus.Value = {
    valueOf(s).getOrElse(StepStatus.ACTIVE)
  }
}

object StepType extends Enumeration {
  val INTRODUCTION, TEXT, QUIZ, TASK, MULTIMEDIA, SUMMARY, TEST = Value

  def valueOf(s: String): Option[StepType.Value] = {
    StepType.values.find(_.toString == s)
  }

  def valueOfOrError(s: String): StepType.Value = {
    valueOf(s) match {
      case Some(stepType) => stepType
      case None =>
        throw new ValidationException(errors = List(ValidationMessage("type", s"'$s' is not a valid steptype.")))
    }
  }

  def valueOfOrDefault(s: String): StepType.Value = {
    valueOf(s).getOrElse(StepType.TEXT)
  }
}
