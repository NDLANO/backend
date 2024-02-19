/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Taxonomy context for the resource")
case class ApiTaxonomyContext(
  @description("Id of the taxonomy object. Legacy field to be removed.") id: String,
  @description("Id of the taxonomy object.") publicId: String,
  @description("Name of the subject this context is in. Legacy field to be removed.") subject: String,
  @description("Name of the root node this context is in.") root: String,
  @description("Id of the subject this context is in. Legacy field to be removed.") subjectId: String,
  @description("Id of the root node this context is in.") rootId: String,
  @description("The relevance for this context.") relevance: String,
  @description("The relevanceId for this context.") relevanceId: String,
  @description("Path to the resource in this context.") path: String,
  @description("Breadcrumbs of path to the resource in this context.") breadcrumbs: List[String],
  @description("Type in this context. Legacy field to be removed.") learningResourceType: String,
  @description("Type in this context.") contextType: String,
  @description("Resource-types of this context.") resourceTypes: List[TaxonomyResourceType],
  @description("Language for this context.") language: String,
  @description("Whether this context is the primary connection. Legacy field to be removed.") isPrimaryConnection: Boolean,
  @description("Whether this context is the primary connection") isPrimary: Boolean,
  @description("Whether this context is active") isActive: Boolean
)
// format: on

object ApiTaxonomyContext {
  implicit val encoder: Encoder[ApiTaxonomyContext] = deriveEncoder
  implicit val decoder: Decoder[ApiTaxonomyContext] = deriveDecoder
}
