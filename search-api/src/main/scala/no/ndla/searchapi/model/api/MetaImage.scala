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

@ApiModel(description = "Meta image of the resource")
case class MetaImage(
    @(ApiModelProperty @field)(description = "The meta image id") url: String,
    @(ApiModelProperty @field)(description = "The meta image alt text") alt: String,
    @(ApiModelProperty @field)(
      description = "The ISO 639-1 language code describing which translation this meta image belongs to"
    ) language: String
) extends LanguageField
