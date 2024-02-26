/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.language.model.WithLanguage
import sttp.tapir.Schema.annotations.description

@description("Description of the article introduction")
case class ArticleIntroduction(
    @description("The introduction content") introduction: String,
    @description(
      "The ISO 639-1 language code describing which article translation this introduction belongs to"
    ) language: String
) extends WithLanguage
