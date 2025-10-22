/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api
import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

@description("Introduction of learningPath")
case class LearningPathIntroductionDTO(
    @description("The freetext introduction of the learningpath")
    introduction: String,
    @description("ISO 639-1 code that represents the language used in introduction")
    language: String,
) extends WithLanguage
