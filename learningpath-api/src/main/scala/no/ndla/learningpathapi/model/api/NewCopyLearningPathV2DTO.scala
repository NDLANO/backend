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
import sttp.tapir.Schema.annotations.description

@description("Meta information for a new learningpath based on a copy")
case class NewCopyLearningPathV2DTO(
    @description("The titles of the learningpath") title: String,
    @description("The descriptions of the learningpath") description: Option[String],
    @description("The chosen language") language: String,
    @description("Url to cover-photo in NDLA image-api.") coverPhotoMetaUrl: Option[String],
    @description("The duration of the learningpath in minutes. Must be greater than 0") duration: Option[Int],
    @description("Searchable tags for the learningpath") tags: Option[Seq[String]],
    @description("Describes the copyright information for the learningpath") copyright: Option[CopyrightDTO]
)

object NewCopyLearningPathV2DTO {
  implicit val encoder: Encoder[NewCopyLearningPathV2DTO] = deriveEncoder
  implicit val decoder: Decoder[NewCopyLearningPathV2DTO] = deriveDecoder
}
