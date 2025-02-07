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
import no.ndla.common.model.api.DraftCopyrightDTO
import sttp.tapir.Schema.annotations.description

@description("Information about the concept")
case class ConceptDTO(
                       // format: off
                       @description("The unique id of the concept") id: Long,
                       @description("The revision of the concept") revision: Int,
                       @description("Available titles for the concept") title: ConceptTitleDTO,
                       @description("The content of the concept") content: Option[ConceptContent],
                       @description("Describes the copyright information for the concept") copyright: Option[DraftCopyrightDTO],
                       @description("URL for the source of the concept") source: Option[String],
                       @description("A meta image for the concept") metaImage: Option[ConceptMetaImageDTO],
                       @description("Search tags the concept is tagged with") tags: Option[ConceptTagsDTO],
                       @description("Taxonomy subject ids the concept is connected to") subjectIds: Option[Set[String]],
                       @description("When the concept was created") created: NDLADate,
                       @description("When the concept was last updated") updated: NDLADate,
                       @description("List of people that updated this concept") updatedBy: Option[Seq[String]],
                       @description("All available languages of the current concept") supportedLanguages: Set[String],
                       @description("Article ids to which the concept is connected to") articleIds: Seq[Long],
                       @description("Status information of the concept") status: StatusDTO,
                       @description("A visual element for the concept") visualElement: Option[VisualElementDTO],
                       @description("Object with data representing the editor responsible for this concept") responsible: Option[ConceptResponsibleDTO],
                       @description("Type of concept. 'concept', or 'gloss'") conceptType: String,
                       @description("Information about the gloss") glossData: Option[GlossDataDTO],
                       @description("Describes the changes made to the concept, only visible to editors") editorNotes: Option[Seq[EditorNoteDTO]],
                       // format: on
)

object ConceptDTO {
  implicit val encoder: Encoder[ConceptDTO] = deriveEncoder
  implicit val decoder: Decoder[ConceptDTO] = deriveDecoder
}
