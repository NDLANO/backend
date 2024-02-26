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
import sttp.tapir.Schema.annotations.description

@description("Meta information for a new learningpath")
case class UpdatedLearningPathV2(
    @description("The revision number for this learningpath") revision: Int,
    @description("The title of the learningpath") title: Option[String],
    @description("The chosen language") language: String,
    @description("The description of the learningpath") description: Option[String],
    @description("Url to cover-photo in NDLA image-api.") coverPhotoMetaUrl: Option[String],
    @description("The duration of the learningpath in minutes. Must be greater than 0") duration: Option[Int],
    @description("Searchable tags for the learningpath") tags: Option[Seq[String]],
    @description("Describes the copyright information for the learningpath") copyright: Option[Copyright],
    @description("Whether to delete a message connected to a learningpath by an administrator.") deleteMessage: Option[
      Boolean
    ]
)

object UpdatedLearningPathV2 {
  implicit val encoder: Encoder[UpdatedLearningPathV2] = deriveEncoder
  implicit val decoder: Decoder[UpdatedLearningPathV2] = deriveDecoder
}
