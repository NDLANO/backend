/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

// format: off
@ApiModel(description = "Taxonomy context for the resource")
case class ApiTaxonomyContext(
  @(ApiModelProperty @field)(description = "Id of the taxonomy object. Legacy field to be removed.") id: String,
  @(ApiModelProperty @field)(description = "Id of the taxonomy object.") publicId: String,
  @(ApiModelProperty @field)(description = "Name of the subject this context is in. Legacy field to be removed.") subject: String,
  @(ApiModelProperty @field)(description = "Name of the root node this context is in.") root: String,
  @(ApiModelProperty @field)(description = "Id of the subject this context is in. Legacy field to be removed.") subjectId: String,
  @(ApiModelProperty @field)(description = "Id of the root node this context is in.") rootId: String,
  @(ApiModelProperty @field)(description = "The relevance for this context.") relevance: String,
  @(ApiModelProperty @field)(description = "The relevanceId for this context.") relevanceId: String,
  @(ApiModelProperty @field)(description = "Path to the resource in this context.") path: String,
  @(ApiModelProperty @field)(description = "Breadcrumbs of path to the resource in this context.") breadcrumbs: List[String],
  @(ApiModelProperty @field)(description = "Type in this context. Legacy field to be removed.") learningResourceType: String,
  @(ApiModelProperty @field)(description = "Type in this context.") contextType: String,
  @(ApiModelProperty @field)(description = "Resource-types of this context.") resourceTypes: List[TaxonomyResourceType],
  @(ApiModelProperty @field)(description = "Language for this context.") language: String,
  @(ApiModelProperty @field)(description = "Whether this context is the primary connection. Legacy field to be removed.") isPrimaryConnection: Boolean,
  @(ApiModelProperty @field)(description = "Whether this context is the primary connection") isPrimary: Boolean,
  @(ApiModelProperty @field)(description = "Whether this context is active") isActive: Boolean
)
// format: on
