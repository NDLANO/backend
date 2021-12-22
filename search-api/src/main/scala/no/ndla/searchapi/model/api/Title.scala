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

@ApiModel(description = "Title of resource")
case class Title(
    @(ApiModelProperty @field)(description = "The freetext title of the resource") title: String,
    @(ApiModelProperty @field)(description = "ISO 639-1 code that represents the language used in title") language: String)
    extends LanguageField
