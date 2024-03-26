/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import enumeratum.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.domain.Title
import no.ndla.common.model.domain.learningpath.EmbedUrl
import no.ndla.language.Language.getSupportedLanguages
import scalikejdbc.*

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
    status: StepStatus = StepStatus.ACTIVE
) {

  def supportedLanguages: Seq[String] = {
    getSupportedLanguages(
      title,
      description,
      embedUrl
    )
  }
}

sealed abstract class StepStatus(override val entryName: String) extends EnumEntry

object StepStatus extends Enum[StepStatus] with CirceEnum[StepStatus] {

  case object ACTIVE  extends StepStatus("ACTIVE")
  case object DELETED extends StepStatus("DELETED")

  def values: IndexedSeq[StepStatus] = findValues

  def valueOf(s: String): Option[StepStatus] = {
    StepStatus.values.find(_.entryName == s)
  }

  def valueOfOrError(status: String): StepStatus = {
    valueOf(status) match {
      case Some(s) => s
      case None =>
        throw new ValidationException(errors = List(ValidationMessage("status", s"'$status' is not a valid status.")))
    }
  }

  def valueOfOrDefault(s: String): StepStatus = {
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
  implicit val encoder: Encoder[StepType.Value] = Encoder.encodeEnumeration(StepType)
  implicit val decoder: Decoder[StepType.Value] = Decoder.decodeEnumeration(StepType)
}

object LearningStep extends SQLSyntaxSupport[LearningStep] {

  implicit val encoder: Encoder[LearningStep] = deriveEncoder
  implicit val decoder: Decoder[LearningStep] = deriveDecoder

  override val tableName = "learningsteps"

  def fromResultSet(ls: SyntaxProvider[LearningStep])(rs: WrappedResultSet): LearningStep =
    fromResultSet(ls.resultName)(rs)

  def fromResultSet(ls: ResultName[LearningStep])(rs: WrappedResultSet): LearningStep = {
    val jsonStr = rs.string(ls.c("document"))
    val meta    = CirceUtil.unsafeParseAs[LearningStep](jsonStr)
    LearningStep(
      Some(rs.long(ls.c("id"))),
      Some(rs.int(ls.c("revision"))),
      rs.stringOpt(ls.c("external_id")),
      Some(rs.long(ls.c("learning_path_id"))),
      meta.seqNo,
      meta.title,
      meta.description,
      meta.embedUrl,
      meta.`type`,
      meta.license,
      meta.showTitle,
      meta.status
    )
  }

  def opt(ls: ResultName[LearningStep])(rs: WrappedResultSet): Option[LearningStep] =
    rs.longOpt(ls.c("id")).map(_ => fromResultSet(ls)(rs))
}
