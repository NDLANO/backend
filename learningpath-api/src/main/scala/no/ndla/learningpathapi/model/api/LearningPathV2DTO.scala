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
import no.ndla.common.model.api.{CommentDTO, ResponsibleDTO}
import sttp.tapir.Schema.annotations.description
import no.ndla.common.model.domain.Priority
import no.ndla.common.model.api.RevisionMetaDTO

@description("Meta information for a learningpath")
case class LearningPathV2DTO(
    @description("The unique id of the learningpath")
    id: Long,
    @description("The revision number for this learningpath")
    revision: Int,
    @description("The id this learningpath is based on, if any")
    isBasedOn: Option[Long],
    @description("The title of the learningpath")
    title: TitleDTO,
    @description("The description of the learningpath")
    description: DescriptionDTO,
    @description("The full url to where the complete metainformation about the learningpath can be found")
    metaUrl: String,
    @description("The learningsteps-summaries for this learningpath")
    learningsteps: Seq[LearningStepV2DTO],
    @description("The full url to where the learningsteps can be found")
    learningstepUrl: String,
    @description("Information about where the cover photo can be found")
    coverPhoto: Option[CoverPhotoDTO],
    @description("The duration of the learningpath in minutes")
    duration: Option[Int],
    @description("The publishing status of the learningpath")
    status: String,
    @description("Verification status")
    verificationStatus: String,
    @description("The date when this learningpath was created.")
    created: NDLADate,
    @description("The date when this learningpath was last updated.")
    lastUpdated: NDLADate,
    @description("Searchable tags for the learningpath")
    tags: LearningPathTagsDTO,
    @description("Describes the copyright information for the learningpath")
    copyright: CopyrightDTO,
    @description("True if authenticated user may edit this learningpath")
    canEdit: Boolean,
    @description("The supported languages for this learningpath")
    supportedLanguages: Seq[String],
    @description("Id identifying the owner of the LearningPath")
    ownerId: Option[String],
    @description("Message set by administrator. Visible if administrator or owner of LearningPath")
    message: Option[MessageDTO],
    @description("The date when this learningpath was made available to the public.")
    madeAvailable: Option[NDLADate],
    @description("Whether the owner of the learningpath is a MyNDLA user or not")
    isMyNDLAOwner: Boolean,
    @description("Object with data representing the editor responsible for this learningpath")
    responsible: Option[ResponsibleDTO],
    @description("Information about comments attached to the learningpath")
    comments: Seq[CommentDTO],
    @description("If the learningpath should be prioritized. Possible values are prioritized, on-hold, unspecified")
    priority: Priority,
    @description("A list of revisions planned for the learningpath")
    revisions: Seq[RevisionMetaDTO],
    @description("An introduction for the learningpath")
    introduction: IntroductionDTO,
    @description("A list of codes from GREP API connected to the article")
    grepCodes: Seq[String],
)

object LearningPathV2DTO {
  implicit val encoder: Encoder[LearningPathV2DTO] = deriveEncoder
  implicit val decoder: Decoder[LearningPathV2DTO] = deriveDecoder
}
