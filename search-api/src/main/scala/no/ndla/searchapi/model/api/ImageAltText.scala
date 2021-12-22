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
case class ImageAltText(@(ApiModelProperty @field)(description = "The freetext alttext of the image") altText: String,
                        @(ApiModelProperty @field)(
                          description = "ISO 639-1 code that represents the language used in alttext") language: String)
    extends LanguageField
