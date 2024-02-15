/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.model.api.{DraftCopyright, UpdateOrDelete}
import sttp.tapir.Schema.annotations.description
import com.scalatsi.TypescriptType.{TSNull, TSUndefined, TSUnion}
import com.scalatsi._

// format: off
@description("Information about the concept")
case class UpdatedConcept(
  @description("The language of this concept") language: String,
  @description("Available titles for the concept") title: Option[String],
  @description("The content of the concept") content: Option[String],
  @description("An image-api ID for the concept meta image") metaImage: UpdateOrDelete[NewConceptMetaImage],
  @description("Describes the copyright information for the concept") copyright: Option[DraftCopyright],
  @description("A list of searchable tags") tags: Option[Seq[String]],
  @description("A list of taxonomy subject ids the concept is connected to") subjectIds: Option[Seq[String]],
  @description("Article id to which the concept is connected to") articleIds: Option[Seq[Long]],
  @description("The new status of the concept") status: Option[String],
  @description("A visual element for the concept. May be anything from an image to a video or H5P") visualElement: Option[String],
  @description("NDLA ID representing the editor responsible for this article") responsibleId: UpdateOrDelete[String],
  @description("Type of concept. 'concept', or 'gloss'") conceptType: Option[String],
  @description("Information about the gloss") glossData: Option[GlossData],
)
// format: on

object UpdatedConcept {
  implicit val encoder: Encoder[UpdatedConcept] = UpdateOrDelete.filterMarkers(deriveEncoder)
  implicit val decoder: Decoder[UpdatedConcept] = deriveDecoder

  implicit val typescriptUpdatedArticle: TSType[UpdatedConcept]    = TSType.fromCaseClass[UpdatedConcept]
  implicit def typescriptNewMetaImage: TSType[NewConceptMetaImage] = TSType.fromCaseClass[NewConceptMetaImage]
  implicit def typescriptNewMetaImageUnion: TSType[UpdateOrDelete[NewConceptMetaImage]] = {
    TSType.alias[UpdateOrDelete[NewConceptMetaImage]](
      "UpdateOrDeleteNewConceptMetaImage",
      TSUnion(Seq(TSNull, TSUndefined, typescriptNewMetaImage.get))
    )
  }
}
