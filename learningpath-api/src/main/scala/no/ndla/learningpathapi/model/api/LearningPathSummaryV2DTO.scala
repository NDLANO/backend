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

@description("Summary of meta information for a learningpath")
case class LearningPathSummaryV2DTO(
    @description("The unique id of the learningpath")
    id: Long,
    @description("The revision number for this learningpath")
    revision: Option[Int],
    @description("The titles of the learningpath")
    title: TitleDTO,
    @description("The descriptions of the learningpath")
    description: DescriptionDTO,
    @description("The introductions of the learningpath")
    introduction: IntroductionDTO,
    @description("The full url to where the complete metainformation about the learningpath can be found")
    metaUrl: String,
    @description("Url to where a cover photo can be found")
    coverPhotoUrl: Option[String],
    @description("The duration of the learningpath in minutes")
    duration: Option[Int],
    @description("The publishing status of the learningpath.")
    status: String,
    @description("The date when this learningpath was created.")
    created: NDLADate,
    @description("The date when this learningpath was last updated.")
    lastUpdated: NDLADate,
    @description("Searchable tags for the learningpath")
    tags: LearningPathTagsDTO,
    @description("The contributors of this learningpath")
    copyright: CopyrightDTO,
    @description("A list of available languages for this audio")
    supportedLanguages: Seq[String],
    @description("The id this learningpath is based on, if any")
    isBasedOn: Option[Long],
    @description(
      "Message that admins can place on a LearningPath for notifying a owner of issues with the LearningPath"
    )
    message: Option[String],
    @description("The codes from GREP API registered for this draft article")
    grepCodes: Seq[String]
)

object LearningPathSummaryV2DTO {
  implicit val encoder: Encoder[LearningPathSummaryV2DTO] = deriveEncoder
  implicit val decoder: Decoder[LearningPathSummaryV2DTO] = deriveDecoder
}
