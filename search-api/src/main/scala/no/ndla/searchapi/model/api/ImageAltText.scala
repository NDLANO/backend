/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api
import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

@description("Title of resource")
case class ImageAltText(
    @description("The freetext alttext of the image") altText: String,
    @description("ISO 639-1 code that represents the language used in alttext") language: String
) extends WithLanguage
