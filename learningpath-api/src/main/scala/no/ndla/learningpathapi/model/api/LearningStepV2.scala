/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.License
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about a learningstep")
case class LearningStepV2(
    @description("The id of the learningstep") id: Long,
    @description("The revision number for this learningstep") revision: Int,
    @description("The sequence number for the step. The first step has seqNo 0.") seqNo: Int,
    @description("The title of the learningstep") title: Title,
    @description("The introduction of the learningstep") introduction: Option[Introduction],
    @description("The description of the learningstep") description: Option[Description],
    @description("The embed content for the learningstep") embedUrl: Option[EmbedUrlV2],
    @description("Determines if the title of the step should be displayed in viewmode") showTitle: Boolean,
    @description("The type of the step") `type`: String,
    @description("Describes the copyright information for the learningstep") license: Option[License],
    @description("The full url to where the complete metainformation about the learningstep can be found") metaUrl: String,
    @description("True if authenticated user may edit this learningstep") canEdit: Boolean,
    @description("The status of the learningstep") status: String,
    @description("The supported languages of the learningstep") supportedLanguages: Seq[String]
)

object LearningStepV2{
  implicit val encoder: Encoder[LearningStepV2] = deriveEncoder
  implicit val decoder: Decoder[LearningStepV2] = deriveDecoder
}
