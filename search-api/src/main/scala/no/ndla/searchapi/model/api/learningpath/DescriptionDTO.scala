/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.language.model.LanguageField
import sttp.tapir.Schema.annotations.description

@description("The description of the learningpath")
case class DescriptionDTO(
    @description("The description to the learningpath.") description: String,
    @description("ISO 639-1 code that represents the language used in introduction") language: String
) extends LanguageField[String] {
  override def value: String    = description
  override def isEmpty: Boolean = description.isEmpty
}
