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
import no.ndla.common.model.api.{DraftCopyrightDTO, UpdateOrDelete}
import sttp.tapir.Schema.annotations.description

@description("Information about the concept")
case class UpdatedConceptDTO(
    @description("The language of this concept")
    language: String,
    @description("Available titles for the concept")
    title: Option[String],
    @description("The content of the concept")
    content: Option[String],
    @description("Describes the copyright information for the concept")
    copyright: Option[DraftCopyrightDTO],
    @description("A list of searchable tags")
    tags: Option[Seq[String]],
    @description("The new status of the concept")
    status: Option[String],
    @description("A visual element for the concept. May be anything from an image to a video or H5P")
    visualElement: Option[String],
    @description("NDLA ID representing the editor responsible for this article")
    responsibleId: UpdateOrDelete[String],
    @description("Type of concept. 'concept', or 'gloss'")
    conceptType: Option[String],
    @description("Information about the gloss")
    glossData: Option[GlossDataDTO]
)

object UpdatedConceptDTO {
  implicit val encoder: Encoder[UpdatedConceptDTO] = UpdateOrDelete.filterMarkers(deriveEncoder)
  implicit val decoder: Decoder[UpdatedConceptDTO] = deriveDecoder
}
