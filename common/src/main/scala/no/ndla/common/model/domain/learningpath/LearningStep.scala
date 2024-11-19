/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.domain.Title
import no.ndla.language.Language.getSupportedLanguages

case class LearningStep(
    id: Option[Long],
    revision: Option[Int],
    externalId: Option[String],
    learningPathId: Option[Long],
    seqNo: Int,
    title: Seq[Title],
    description: Seq[Description],
    embedUrl: Seq[EmbedUrl],
    `type`: StepType,
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

object LearningStep {
  implicit val encoder: Encoder[LearningStep] = deriveEncoder
  implicit val decoder: Decoder[LearningStep] = deriveDecoder
}
