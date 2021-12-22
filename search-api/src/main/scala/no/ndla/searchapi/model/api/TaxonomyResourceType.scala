/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Taxonomy resource type")
case class TaxonomyResourceType(
    @(ApiModelProperty @field)(description = "Id of the taoxonomy resource type") id: String,
    @(ApiModelProperty @field)(description = "Name of the subject this context is in") name: String,
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which article translation this visual element belongs to") language: String)
