/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.tapir.Schema.annotations.description

@description("Taxonomy context for the resource")
case class ApiTaxonomyContext(
    @description("Id of the taxonomy object.") publicId: String,
    @description("Name of the root node this context is in.") root: String,
    @description("Id of the root node this context is in.") rootId: String,
    @description("The relevance for this context.") relevance: String,
    @description("The relevanceId for this context.") relevanceId: String,
    @description("Path to the resource in this context.") path: Option[String],
    @description("Breadcrumbs of path to the resource in this context.") breadcrumbs: List[String],
    @description("Unique id of this context.") contextId: String,
    @description("Type in this context.") contextType: String,
    @description("Resource-types of this context.") resourceTypes: List[TaxonomyResourceType],
    @description("Language for this context.") language: String,
    @description("Whether this context is the primary connection") isPrimary: Boolean,
    @description("Whether this context is active") isActive: Boolean,
    @description("Unique url for this context.") url: Option[String]
)

object ApiTaxonomyContext {
  implicit val encoder: Encoder[ApiTaxonomyContext] = deriveEncoder
  implicit val decoder: Decoder[ApiTaxonomyContext] = deriveDecoder
}
