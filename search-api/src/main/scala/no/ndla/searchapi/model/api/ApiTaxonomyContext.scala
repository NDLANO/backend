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
  @(ApiModelProperty @field)(description = "Id of the taxonomy object") id: String,
  @(ApiModelProperty @field)(description = "Id of the taxonomy object") publicId: String,
  @(ApiModelProperty @field)(description = "Name of the subject this context is in") subject: String,
  @(ApiModelProperty @field)(description = "Name of the subject this context is in") root: String,
  @(ApiModelProperty @field)(description = "Id of the subject this context is in") subjectId: String,
  @(ApiModelProperty @field)(description = "Id of the subject this context is in") rootId: String,
  @(ApiModelProperty @field)(description = "The relevance for this context") relevance: String,
  @(ApiModelProperty @field)(description = "Path to the resource in this context") path: String,
  @(ApiModelProperty @field)(description = "Breadcrumbs of path to the resource in this context") breadcrumbs: List[String],
  @(ApiModelProperty @field)(description = "Type in this context.") learningResourceType: String,
  @(ApiModelProperty @field)(description = "Type in this context.") contextType: String,
  @(ApiModelProperty @field)(description = "Resource-types of this context.") resourceTypes: List[TaxonomyResourceType],
  @(ApiModelProperty @field)(description = "Language for this context") language: String,
  @(ApiModelProperty @field)(description = "Whether this context is the primary connection") isPrimaryConnection: Boolean,
  @(ApiModelProperty @field)(description = "Whether this context is the primary connection") isPrimary: Boolean
)
// format: on
