/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.learningpath.EmbedUrl
import no.ndla.common.model.domain.Title

case class LearningStep(
    id: Option[Long],
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
    status: StepStatus.Value = StepStatus.ACTIVE
) {}

object LearningStep {
  implicit val encoder: Encoder[LearningStep] = deriveEncoder
  implicit val decoder: Decoder[LearningStep] = deriveDecoder
}

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
  implicit val encoder: Encoder[StepStatus.Value] = Encoder.encodeEnumeration(StepStatus)
  implicit val decoder: Decoder[StepStatus.Value] = Decoder.decodeEnumeration(StepStatus)
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
  implicit val encoder: Encoder[StepType.Value] = Encoder.encodeEnumeration(StepType)
  implicit val decoder: Decoder[StepType.Value] = Decoder.decodeEnumeration(StepType)
}
