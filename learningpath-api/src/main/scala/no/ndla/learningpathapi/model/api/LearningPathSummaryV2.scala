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
import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

// format: off
@description("Summary of meta information for a learningpath")
case class LearningPathSummaryV2(
    @description("The unique id of the learningpath") id: Long,
    @description("The revision number for this learningpath") revision: Option[Int],
    @description("The titles of the learningpath") title: Title,
    @description("The descriptions of the learningpath") description: Description,
    @description("The introductions of the learningpath") introduction: Introduction,
    @description("The full url to where the complete metainformation about the learningpath can be found") metaUrl: String,
    @description("Url to where a cover photo can be found") coverPhotoUrl: Option[String],
    @description("The duration of the learningpath in minutes") duration: Option[Int],
    @description("The publishing status of the learningpath.") status: String,
    @description("The date when this learningpath was last updated.") lastUpdated: NDLADate,
    @description("Searchable tags for the learningpath") tags: LearningPathTags,
    @description("The contributors of this learningpath") copyright: Copyright,
    @description("A list of available languages for this audio") supportedLanguages: Seq[String],
    @description("The id this learningpath is based on, if any") isBasedOn: Option[Long],
    @description("Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath") message: Option[String]
)

object LearningPathSummaryV2 {
  implicit val encoder: Encoder[LearningPathSummaryV2] = deriveEncoder
  implicit val decoder: Decoder[LearningPathSummaryV2] = deriveDecoder
}
