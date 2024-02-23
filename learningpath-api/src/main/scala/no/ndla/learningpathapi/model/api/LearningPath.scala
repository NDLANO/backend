/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

// format: off
@description("Meta information for a learningpath")
case class LearningPathV2(
    @description("The unique id of the learningpath") id: Long,
    @description("The revision number for this learningpath") revision: Int,
    @description("The id this learningpath is based on, if any") isBasedOn: Option[Long],
    @description("The title of the learningpath") title: Title,
    @description("The description of the learningpath") description: Description,
    @description("The full url to where the complete metainformation about the learningpath can be found") metaUrl: String,
    @description("The learningsteps-summaries for this learningpath") learningsteps: Seq[LearningStepV2],
    @description("The full url to where the learningsteps can be found") learningstepUrl: String,
    @description("Information about where the cover photo can be found") coverPhoto: Option[CoverPhoto],
    @description("The duration of the learningpath in minutes") duration: Option[Int],
    @description("The publishing status of the learningpath") status: String,
    @description("Verification status") verificationStatus: String,
    @description("The date when this learningpath was last updated.") lastUpdated: NDLADate,
    @description("Searchable tags for the learningpath") tags: LearningPathTags,
    @description("Describes the copyright information for the learningpath") copyright: Copyright,
    @description("True if authenticated user may edit this learningpath") canEdit: Boolean,
    @description("The supported languages for this learningpath") supportedLanguages: Seq[String],
    @description("Visible if administrator or owner of LearningPath") ownerId: Option[String],
    @description("Message set by administrator. Visible if administrator or owner of LearningPath") message: Option[Message]
)

object LearningPathV2 {
  implicit val encoder: Encoder[LearningPathV2] = deriveEncoder
  implicit val decoder: Decoder[LearningPathV2] = deriveDecoder
}
