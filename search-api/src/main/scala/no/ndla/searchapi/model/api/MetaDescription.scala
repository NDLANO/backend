/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import no.ndla.searchapi.model.domain.LanguageField
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Meta description of the resource")
case class MetaDescription(
    @(ApiModelProperty @field)(description = "The meta description") metaDescription: String,
    @(ApiModelProperty @field)(description =
      "The ISO 639-1 language code describing which article translation this meta description belongs to") language: String)
    extends LanguageField
