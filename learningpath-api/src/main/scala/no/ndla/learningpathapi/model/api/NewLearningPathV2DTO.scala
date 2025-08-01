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
import no.ndla.common.model.api.NewCommentDTO
import sttp.tapir.Schema.annotations.description

@description("Meta information for a new learningpath")
case class NewLearningPathV2DTO(
    @description("The titles of the learningpath") title: String,
    @description("The descriptions of the learningpath") description: Option[String],
    @description("Url to cover-photo in NDLA image-api.") coverPhotoMetaUrl: Option[String],
    @description("The duration of the learningpath in minutes. Must be greater than 0") duration: Option[Int],
    @description("Searchable tags for the learningpath") tags: Option[Seq[String]],
    @description("The chosen language") language: String,
    @description("Describes the copyright information for the learningpath") copyright: Option[CopyrightDTO],
    @description("NDLA ID representing the editor responsible for this learningpath") responsibleId: Option[String],
    @description("Information about comments attached to the learningpath") comments: Option[List[NewCommentDTO]]
)

object NewLearningPathV2DTO {
  implicit val encoder: Encoder[NewLearningPathV2DTO] = deriveEncoder
  implicit val decoder: Decoder[NewLearningPathV2DTO] = deriveDecoder
}
