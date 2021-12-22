/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta image for the concept")
case class ConceptTags(
    @(ApiModelProperty @field)(description = "Searchable tags") tags: Seq[String],
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which concept translation these tags belongs to") language: String)
