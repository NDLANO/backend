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
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{DraftCopyrightDTO, ResponsibleDTO}
import sttp.tapir.Schema.annotations.description

@description("Information about the concept")
case class ConceptSummaryDTO(
    @description("The unique id of the concept")
    id: Long,
    @description("Available titles for the concept")
    title: ConceptTitleDTO,
    @description("The content of the concept in available languages")
    content: ConceptContent,
    @description("Search tags the concept is tagged with")
    tags: Option[ConceptTagsDTO],
    @description("All available languages of the current concept")
    supportedLanguages: Seq[String],
    @description("The time when the article was last updated")
    lastUpdated: NDLADate,
    @description("When the concept was created")
    created: NDLADate,
    @description("Status information of the concept")
    status: StatusDTO,
    @description("List of people that edited the concept")
    updatedBy: Seq[String],
    @description("Describes the license of the concept")
    license: Option[String],
    @description("Describes the copyright of the concept")
    copyright: Option[DraftCopyrightDTO],
    @description("A visual element for the concept")
    visualElement: Option[VisualElementDTO],
    @description("URL for the source of the concept")
    source: Option[String],
    @description("Object with data representing the editor responsible for this concept")
    responsible: Option[ResponsibleDTO],
    @description("Type of concept. 'concept', or 'gloss'")
    conceptType: String,
    @description("Information about the gloss")
    glossData: Option[GlossDataDTO],
    @description("A translated name of the concept type")
    conceptTypeName: String,
)

object ConceptSummaryDTO {
  implicit val encoder: Encoder[ConceptSummaryDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptSummaryDTO] = deriveDecoder
}
