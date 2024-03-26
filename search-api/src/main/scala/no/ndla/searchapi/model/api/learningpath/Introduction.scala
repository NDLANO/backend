/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

@description("The introduction of the learningpath")
case class Introduction(
    @description("The introduction to the learningpath. Basic HTML allowed") introduction: String,
    @description("ISO 639-1 code that represents the language used in introduction") language: String
) extends WithLanguage
