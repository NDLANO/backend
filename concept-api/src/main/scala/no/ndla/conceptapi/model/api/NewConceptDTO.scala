/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.DraftCopyrightDTO
import sttp.tapir.Schema.annotations.description

@description("Information about the concept")
case class NewConceptDTO(
    @description("The language of this concept")
    language: String,
    @description("Available titles for the concept")
    title: String,
    @description("The content of the concept")
    content: Option[String],
    @description("Describes the copyright information for the concept")
    copyright: Option[DraftCopyrightDTO],
    @description("A list of searchable tags")
    tags: Option[Seq[String]],
    @description("A visual element for the concept. May be anything from an image to a video or H5P")
    visualElement: Option[String],
    @description("NDLA ID representing the editor responsible for this article")
    responsibleId: Option[String],
    @description("Type of concept. 'concept', or 'gloss'")
    conceptType: String,
    @description("Information about the gloss")
    glossData: Option[GlossDataDTO],
)

object NewConceptDTO {
  implicit val encoder: Encoder[NewConceptDTO] = deriveEncoder
  implicit val decoder: Decoder[NewConceptDTO] = deriveDecoder
}
