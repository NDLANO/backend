/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.DraftCopyright
import sttp.tapir.Schema.annotations.description

// format: off
@description("Information about the concept")
case class NewConcept(
  @description("The language of this concept") language: String,
  @description("Available titles for the concept") title: String,
  @description("The content of the concept") content: Option[String],
  @description("Describes the copyright information for the concept") copyright: Option[DraftCopyright],
  @description("An image-api ID for the concept meta image") metaImage: Option[NewConceptMetaImage],
  @description("A list of searchable tags") tags: Option[Seq[String]],
  @description("A list of taxonomy subject ids the concept is connected to") subjectIds: Option[Seq[String]],
  @description("Article id to which the concept is connected to") articleIds: Option[Seq[Long]],
  @description("A visual element for the concept. May be anything from an image to a video or H5P") visualElement: Option[String],
  @description("NDLA ID representing the editor responsible for this article") responsibleId: Option[String],
  @description("Type of concept. 'concept', or 'gloss'") conceptType: String,
  @description("Information about the gloss") glossData: Option[GlossData],
)
// format: on

object NewConcept {
  implicit val encoder: Encoder[NewConcept] = deriveEncoder
  implicit val decoder: Decoder[NewConcept] = deriveDecoder
}
