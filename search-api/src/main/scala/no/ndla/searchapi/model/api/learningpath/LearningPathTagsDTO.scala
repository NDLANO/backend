/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

case class LearningPathTagsDTO(
    @description("The searchable tags. Must be plain text")
    tags: Seq[String],
    @description("ISO 639-1 code that represents the language used in tag")
    language: String,
) extends WithLanguage
